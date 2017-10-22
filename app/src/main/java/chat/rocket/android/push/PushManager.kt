package chat.rocket.android.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.util.SparseArray
import chat.rocket.android.BackgroundLooper
import chat.rocket.android.BuildConfig
import chat.rocket.android.R
import chat.rocket.android.RocketChatCache
import chat.rocket.android.activity.MainActivity
import chat.rocket.android.helper.Logger
import chat.rocket.core.interactors.MessageInteractor
import chat.rocket.core.models.Room
import chat.rocket.core.models.User
import chat.rocket.persistence.realm.repositories.RealmMessageRepository
import chat.rocket.persistence.realm.repositories.RealmRoomRepository
import chat.rocket.persistence.realm.repositories.RealmUserRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import org.json.JSONObject
import java.io.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

typealias TupleRoomUser = Pair<Room, User>
typealias TupleGroupIdMessageCount = Pair<Int, AtomicInteger>

object PushManager {
    const val EXTRA_NOT_ID = "chat.rocket.android.EXTRA_NOT_ID"
    const val EXTRA_HOSTNAME = "chat.rocket.android.EXTRA_HOSTNAME"
    const val EXTRA_PUSH_MESSAGE = "chat.rocket.android.EXTRA_PUSH_MESSAGE"
    const val EXTRA_ROOM_ID = "chat.rocket.android.EXTRA_ROOM_ID"
    private const val REPLY_LABEL = "REPLY"
    private const val REMOTE_INPUT_REPLY = "REMOTE_INPUT_REPLY"

    // Map associating a notification id to a list of corresponding messages ie. an id corresponds
    // to a user and the corresponding list is all the messages sent by him.
    private val messageStack = SparseArray<MutableList<CharSequence>>()

    // Notifications received from the same server are grouped in a single bundled notification.
    // This map associates a host to a group id.
    private val groupMap = HashMap<String, TupleGroupIdMessageCount>()

    // Map a hostname to a list of push messages that pertain to it.
    private val hostToPushMessageList = HashMap<String, MutableList<PushMessage>>()
    private val randomizer = Random()

    /**
     * Handles a receiving push by creating and displaying an appropriate notification based
     * on the *data* param bundle received.
     */
    @Synchronized
    fun handle(context: Context, data: Bundle) {
        val appContext = context.applicationContext
        val message = data["message"] as String
        val image = data["image"] as String
        val ejson = data["ejson"] as String
        val notId = data["notId"] as String
        val style = data["style"] as String
        val summaryText = data["summaryText"] as String
        val count = data["count"] as String
        val title = data["title"] as String
        val lastPushMessage = PushMessage(title, message, image, ejson, count, notId, summaryText, style)

        // We should use Timber here
        if (BuildConfig.DEBUG) {
            Log.d(PushMessage::class.java.simpleName, lastPushMessage.toString())
        }

        bundleMessage(notId.toInt(), lastPushMessage.message)

        showNotification(appContext, lastPushMessage)
    }

    /**
     * Clear all messages corresponding to a specific notification id (aka specific user)
     */
    fun clearNotificationIdStack(notificationId: Int) {
        messageStack.delete(notificationId)
    }

    fun clearHostNotifications(host: String) {
        hostToPushMessageList.remove(host)
    }

    private fun showNotification(context: Context, lastPushMessage: PushMessage) {
        val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notId = lastPushMessage.notificationId.toInt()
        val host = lastPushMessage.host
        val groupTuple = getGroupForHost(host)

        groupTuple.second.incrementAndGet()
        if (isAndroidVersionAtLeast(Build.VERSION_CODES.O)) {
            val notification = createSingleNotificationForOreo(context, lastPushMessage)
            val groupNotification = createGroupNotificationForOreo(context, lastPushMessage)
            manager.notify(notId, notification)
            manager.notify(groupTuple.first, groupNotification)
        } else {
            val notIdListForHostname: MutableList<PushMessage>? = hostToPushMessageList.get(host)
            if (notIdListForHostname == null) {
                hostToPushMessageList.put(host, arrayListOf(lastPushMessage))
            } else {
                notIdListForHostname.add(lastPushMessage)
            }

            val notification = createSingleNotification(context, lastPushMessage)
            val groupNotification = createGroupNotification(context, lastPushMessage)
            NotificationManagerCompat.from(context).notify(notId, notification)
            NotificationManagerCompat.from(context).notify(groupTuple.first, groupNotification)
        }
    }

    private fun isAndroidVersionAtLeast(minVersion: Int) = Build.VERSION.SDK_INT >= minVersion

    private fun getGroupForHost(host: String): TupleGroupIdMessageCount {
        val size = groupMap.size
        var group = groupMap.get(host)
        if (group == null) {
            group = TupleGroupIdMessageCount(size + 1, AtomicInteger(0))
            groupMap.put(host, group)
        }
        return group
    }

    private fun createGroupNotification(context: Context, lastPushMessage: PushMessage): Notification {
        with(lastPushMessage) {
            val id = lastPushMessage.notificationId.toInt()
            val contentIntent = getContentIntent(context, id, lastPushMessage, singleConversation = true)
            val deleteIntent = getDismissIntent(context, lastPushMessage)
            val builder = NotificationCompat.Builder(context)
                    .setWhen(createdAt)
                    .setContentTitle(title.fromHtml())
                    .setContentText(message.fromHtml())
                    .setGroup(host)
                    .setGroupSummary(true)
                    .setContentIntent(contentIntent)
                    .setDeleteIntent(deleteIntent)
                    .setMessageNotification()

            val subText = RocketChatCache(context).getHostSiteName(host)
            if (subText.isNotEmpty()) {
                builder.setSubText(subText)
            }

            if (style == "inbox") {
                val pushMessageList = hostToPushMessageList.get(host)

                pushMessageList?.let {
                    val messageCount = pushMessageList.size
                    val summary = summaryText.replace("%n%", messageCount.toString())
                            .fromHtml()
                    builder.setNumber(messageCount)
                    if (messageCount > 1) {
                        val firstPush = pushMessageList[0]
                        val singleConversation = pushMessageList.filter {
                            firstPush.sender.username != it.sender.username
                        }.isEmpty()

                        val inbox = NotificationCompat.InboxStyle()
                                .setBigContentTitle(if (singleConversation) title else summary)

                        for (push in pushMessageList) {
                            if (singleConversation) {
                                inbox.addLine(push.message)
                            } else {
                                inbox.addLine("<font color='black'>${push.title}</font> <font color='gray'>${push.message}</font>".fromHtml())
                            }
                        }

                        builder.setStyle(inbox)
                    } else {
                        val bigText = NotificationCompat.BigTextStyle()
                                .bigText(pushMessageList[0].message.fromHtml())
                                .setBigContentTitle(pushMessageList[0].title.fromHtml())

                        builder.setStyle(bigText)
                    }
                }
            } else {
                val bigText = NotificationCompat.BigTextStyle()
                        .bigText(message.fromHtml())
                        .setBigContentTitle(title.fromHtml())

                builder.setStyle(bigText)
            }

            return builder.build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGroupNotificationForOreo(context: Context, lastPushMessage: PushMessage): Notification {
        with(lastPushMessage) {
            val manager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val id = notificationId.toInt()
            val contentIntent = getContentIntent(context, id, lastPushMessage, singleConversation = true)
            val deleteIntent = getDismissIntent(context, lastPushMessage)
            val channelGroup = NotificationChannel(host, host, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channelGroup)
            val builder = Notification.Builder(context, host)
                    .setWhen(createdAt)
                    .setContentTitle(title.fromHtml())
                    .setContentText(message.fromHtml())
                    .setGroup(host)
                    .setGroupSummary(true)
                    .setNumber(count.toInt())
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setContentIntent(contentIntent)
                    .setDeleteIntent(deleteIntent)
                    .setMessageNotification(context)

            val subText = RocketChatCache(context).getHostSiteName(host)
            if (subText.isNotEmpty()) {
                builder.setSubText(subText)
            }

            val messages = messageStack.get(notificationId.toInt())
            val messageCount = messages.size

            if (messageCount > 1) {
                val summary = summaryText.replace("%n%", messageCount.toString())
                val inbox = Notification.InboxStyle()
                        .setBigContentTitle(title.fromHtml())
                        .setSummaryText(summary)

                builder.setStyle(inbox)
            } else {
                val bigText = Notification.BigTextStyle()
                        .bigText(message.fromHtml())
                        .setBigContentTitle(title.fromHtml())

                builder.setStyle(bigText)
            }

            return builder.build()
        }
    }

    private fun createSingleNotification(context: Context, lastPushMessage: PushMessage): Notification {
        with(lastPushMessage) {
            val id = notificationId.toInt()
            val contentIntent = getContentIntent(context, id, lastPushMessage)
            val deleteIntent = getDismissIntent(context, lastPushMessage)

            val builder = NotificationCompat.Builder(context)
                    .setWhen(createdAt)
                    .setContentTitle(title.fromHtml())
                    .setContentText(message.fromHtml())
                    .setGroupSummary(false)
                    .setGroup(host)
                    .setDeleteIntent(deleteIntent)
                    .setContentIntent(contentIntent)
                    .setMessageNotification()
                    .addReplyAction(lastPushMessage)

            val subText = RocketChatCache(context).getHostSiteName(lastPushMessage.host)
            if (subText.isNotEmpty()) {
                builder.setSubText(subText)
            }

            val pushMessageList = hostToPushMessageList.get(host)

            pushMessageList?.let {
                if (pushMessageList.isNotEmpty()) {
                    val messageCount = pushMessageList.size
                    val bigText = NotificationCompat.BigTextStyle()
                            .bigText(pushMessageList.last().message.fromHtml())
                            .setBigContentTitle(pushMessageList.last().title.fromHtml())

                    builder.setStyle(bigText).setNumber(messageCount)
                }
            }

            return builder.build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSingleNotificationForOreo(context: Context, lastPushMessage: PushMessage): Notification {
        val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        with(lastPushMessage) {
            val id = notificationId.toInt()
            val contentIntent = getContentIntent(context, id, lastPushMessage)
            val deleteIntent = getDismissIntent(context, lastPushMessage)

            val channel = NotificationChannel(host, host, NotificationManager.IMPORTANCE_DEFAULT)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setShowBadge(true)
            manager.createNotificationChannel(channel)
            val builder = Notification.Builder(context, host)
                    .setWhen(createdAt)
                    .setContentTitle(title.fromHtml())
                    .setContentText(message.fromHtml())
                    .setGroup(host)
                    .setGroupSummary(false)
                    .setDeleteIntent(deleteIntent)
                    .setContentIntent(contentIntent)
                    .setMessageNotification(context)
                    .addReplyAction(context, lastPushMessage)

            val subText = RocketChatCache(context).getHostSiteName(lastPushMessage.host)
            if (subText.isNotEmpty()) {
                builder.setSubText(subText)
            }

            if ("inbox" == style) {
                val messages = messageStack.get(notificationId.toInt())
                val messageCount = messages.size
                if (messageCount > 1) {
                    val summary = summaryText.replace("%n%", messageCount.toString())
                            .fromHtml()
                    val inbox = Notification.InboxStyle()
                            .setBigContentTitle(title.fromHtml())
                            .setSummaryText(summary)

                    messages.forEach { msg ->
                        inbox.addLine(msg.fromHtml())
                    }

                    builder.setStyle(inbox)
                } else {
                    val bigText = Notification.BigTextStyle()
                            .bigText(message.fromHtml())
                            .setBigContentTitle(title.fromHtml())

                    builder.setStyle(bigText)
                }
            } else {
                builder.setContentText(message.fromHtml())
            }

            return builder.build()
        }
    }

    private fun bundleMessage(id: Int, message: CharSequence) {
        val existingStack: MutableList<CharSequence>? = messageStack[id]

        if (existingStack == null) {
            val newStack = arrayListOf<CharSequence>()
            newStack.add(message)
            messageStack.put(id, newStack)
        } else {
            existingStack.add(0, message)
        }
    }

    private fun getDismissIntent(context: Context, pushMessage: PushMessage): PendingIntent {
        val deleteIntent = Intent(context, DeleteReceiver::class.java)
        deleteIntent.putExtra(EXTRA_NOT_ID, pushMessage.notificationId.toInt())
        deleteIntent.putExtra(EXTRA_HOSTNAME, pushMessage.host)
        return PendingIntent.getBroadcast(context, pushMessage.notificationId.toInt(), deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getContentIntent(context: Context, notificationId: Int, pushMessage: PushMessage, singleConversation: Boolean = true): PendingIntent {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notificationIntent.putExtra(EXTRA_NOT_ID, notificationId)
        notificationIntent.putExtra(EXTRA_HOSTNAME, pushMessage.host)
        if (singleConversation) {
            notificationIntent.putExtra(EXTRA_ROOM_ID, pushMessage.rid)
        }
        return PendingIntent.getActivity(context, randomizer.nextInt(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // CharSequence extensions
    private fun CharSequence.fromHtml(): Spanned {
        return Html.fromHtml(this as String)
    }

    //Notification.Builder extensions
    @RequiresApi(Build.VERSION_CODES.N)
    private fun Notification.Builder.addReplyAction(ctx: Context, pushMessage: PushMessage): Notification.Builder {
        val replyRemoteInput = android.app.RemoteInput.Builder(REMOTE_INPUT_REPLY)
                .setLabel(REPLY_LABEL)
                .build()
        val replyIntent = Intent(ctx, ReplyReceiver::class.java)
        replyIntent.putExtra("push", pushMessage as Serializable)
        val pendingIntent = PendingIntent.getBroadcast(
                ctx, randomizer.nextInt(), replyIntent, 0)
        val replyAction =
                Notification.Action.Builder(
                        Icon.createWithResource(ctx, R.drawable.ic_reply), REPLY_LABEL, pendingIntent)
                        .addRemoteInput(replyRemoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()
        this.addAction(replyAction)
        return this
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Notification.Builder.setMessageNotification(ctx: Context): Notification.Builder {
        val res = ctx.resources
        val smallIcon = res.getIdentifier(
                "rocket_chat_notification", "drawable", ctx.packageName)
        with(this, {
            setAutoCancel(true)
            setShowWhen(true)
            setColor(res.getColor(R.color.colorRed400, ctx.theme))
            setSmallIcon(smallIcon)
        })
        return this
    }

    // NotificationCompat.Builder extensions
    private fun NotificationCompat.Builder.addReplyAction(pushMessage: PushMessage): NotificationCompat.Builder {
        val context = this.mContext
        val replyRemoteInput = RemoteInput.Builder(REMOTE_INPUT_REPLY)
                .setLabel(REPLY_LABEL)
                .build()
        val replyIntent = Intent(context, ReplyReceiver::class.java)
        replyIntent.putExtra(EXTRA_PUSH_MESSAGE, pushMessage as Serializable)
        val pendingIntent = PendingIntent.getBroadcast(
                context, randomizer.nextInt(), replyIntent, 0)
        val replyAction =
                NotificationCompat.Action.Builder(
                        R.drawable.ic_reply, REPLY_LABEL, pendingIntent)
                        .addRemoteInput(replyRemoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()
        this.addAction(replyAction)
        return this
    }

    private fun NotificationCompat.Builder.setMessageNotification(): NotificationCompat.Builder {
        val ctx = this.mContext
        val res = ctx.resources
        val smallIcon = res.getIdentifier(
                "rocket_chat_notification", "drawable", ctx.packageName)
        with(this, {
            setAutoCancel(true)
            setShowWhen(true)
            setColor(ctx.resources.getColor(R.color.colorRed400))
            setDefaults(Notification.DEFAULT_ALL)
            setSmallIcon(smallIcon)
        })
        return this
    }

    private data class PushMessage(
           val title: String,
           val message: String,
           val image: String?,
           val ejson: String,
           val count: String,
           val notificationId: String,
           val summaryText: String,
           val style: String) : Serializable {
        val host: String
        val rid: String
        val type: String
        val name: String?
        val sender: Sender
        val createdAt: Long

        init {
            val json = JSONObject(ejson)
            host = json.getString("host")
            rid = json.getString("rid")
            type = json.getString("type")
            name = json.optString("name")
            sender = Sender(json.getString("sender"))
            createdAt = System.currentTimeMillis()
        }

        data class Sender(val sender: String) : Serializable {
            val _id: String
            val username: String
            val name: String

            init {
                val json = JSONObject(sender)
                _id = json.getString("_id")
                username = json.getString("username")
                name = json.getString("name")
            }
        }
    }

    /**
     * BroadcastReceiver for dismissed notifications.
     */
    class DeleteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val notificationId = intent?.extras?.getInt(EXTRA_NOT_ID)
            val host = intent?.extras?.getString(EXTRA_HOSTNAME)
            notificationId?.let {
                clearNotificationIdStack(notificationId)
            }
            host?.let {
                clearHostNotifications(host)
            }
        }
    }

    /**
     * *EXPERIMENTAL*
     *
     * BroadcastReceiver for notifications' replies using Direct Reply feature (Android >= 7).
     */
    class ReplyReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null) {
                return
            }

            synchronized(this) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val message: CharSequence? = extractMessage(intent)
                val pushMessage = intent?.extras?.getSerializable(EXTRA_PUSH_MESSAGE) as PushMessage?

                pushMessage?.let {
                    val userNotId = pushMessage.notificationId.toInt()
                    val groupTuple = groupMap.get(pushMessage.host)
                    messageStack[userNotId]?.let {
                        for (msg in messageStack[userNotId]) {
                            manager.cancel(userNotId)
                            groupTuple?.second?.decrementAndGet()
                        }
                        clearNotificationIdStack(userNotId)
                        groupTuple?.let {
                            val groupNotId = groupTuple.first
                            val totalNot = groupTuple.second.get()
                            if (totalNot == 0) {
                                manager.cancel(groupNotId)
                            }
                        }
                        message?.let {
                            sendMessage(context, message, pushMessage.rid)
                        }
                    }
                }
            }
        }

        private fun extractMessage(intent: Intent?): CharSequence? {
            val remoteInput: Bundle? =
                    RemoteInput.getResultsFromIntent(intent)
            return remoteInput?.getCharSequence(REMOTE_INPUT_REPLY)
        }

        // Just kept for reference. We should use this on rewrite with job schedulers
        private fun sendMessage(ctx: Context, message: CharSequence, roomId: String) {
            val hostname = RocketChatCache(ctx).selectedServerHostname
            val roomRepository = RealmRoomRepository(hostname)
            val userRepository = RealmUserRepository(hostname)
            val messageRepository = RealmMessageRepository(hostname)
            val messageInteractor = MessageInteractor(messageRepository, roomRepository)

            val singleRoom: Single<Room> = roomRepository.getById(roomId)
                    .filter({ it.isPresent })
                    .map({ it.get() })
                    .firstElement()
                    .toSingle()

            val singleUser: Single<User> = userRepository.getCurrent()
                    .filter({ it.isPresent })
                    .map({ it.get() })
                    .firstElement()
                    .toSingle()

            val roomUserTuple: Single<TupleRoomUser> = Single.zip(
                    singleRoom,
                    singleUser,
                    BiFunction { room, user -> TupleRoomUser(room, user) })

            roomUserTuple.flatMap { tuple -> messageInteractor.send(tuple.first, tuple.second, message as String) }
                    .subscribeOn(AndroidSchedulers.from(BackgroundLooper.get()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { success ->
                                // Empty
                            },
                            { throwable ->
                                Logger.report(throwable)
                            })
        }
    }
}