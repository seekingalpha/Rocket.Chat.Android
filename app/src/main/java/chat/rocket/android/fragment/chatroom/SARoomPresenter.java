package chat.rocket.android.fragment.chatroom;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.google.gson.Gson;
import com.seekingalpha.sanetwork.TrackingHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chat.rocket.android.BackgroundLooper;
import chat.rocket.android.api.MethodCallHelper;
import chat.rocket.android.helper.AbsoluteUrlHelper;
import chat.rocket.android.helper.Logger;
import chat.rocket.android.service.ConnectivityManagerApi;
import chat.rocket.core.interactors.MessageInteractor;
import chat.rocket.core.models.Room;
import chat.rocket.core.models.User;
import chat.rocket.core.repositories.RoomRepository;
import chat.rocket.core.repositories.UserRepository;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SARoomPresenter extends RoomPresenter {

    private TrackingHelper trackingHelper;
    private MessageInteractor messageInteractor;

    public SARoomPresenter(
            String roomId,
            UserRepository userRepository,
            MessageInteractor messageInteractor,
            RoomRepository roomRepository,
            AbsoluteUrlHelper absoluteUrlHelper,
            MethodCallHelper methodCallHelper,
            ConnectivityManagerApi connectivityManagerApi,
            TrackingHelper trackingHelper
    ) {
        super(roomId, userRepository, messageInteractor, roomRepository, absoluteUrlHelper, methodCallHelper, connectivityManagerApi);
        this.messageInteractor = messageInteractor;
        this.trackingHelper = trackingHelper;
    }

    @Override
    public void bindView(@NonNull RoomContract.View view) {
        super.bindView(view);
        final Disposable subscription = getRoomUserPair()
                .subscribeOn(AndroidSchedulers.from(BackgroundLooper.get()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pair -> trackRoom(pair.first),
                        Logger.INSTANCE::report
                );

        addSubscription(subscription);
    }

    @Override
    public void sendMessage(String messageText) {
        final Disposable subscription = getRoomUserPair()
                .subscribeOn(AndroidSchedulers.from(BackgroundLooper.get()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pair -> {
                            Disposable send = messageInteractor.send(pair.first, pair.second, messageText)
                                    .subscribeOn(AndroidSchedulers.from(BackgroundLooper.get()))
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            success -> {
                                                if (success) {
                                                    trackMessage(pair, messageText);
                                                    view.onMessageSendSuccessfully();
                                                }
                                            },
                                            Logger.INSTANCE::report
                                    );
                            addSubscription(send);
                        },
                        Logger.INSTANCE::report
                );

        addSubscription(subscription);
    }

    public void trackMessage(Pair<Room, User> pair, String msg) {
        switch (pair.first.getType()) {
            case Room.TYPE_DIRECT_MESSAGE:
                trackingHelper.sendsDirectMessageEvent();
                break;
            case Room.TYPE_GROUP:
                trackingHelper.sendsDirectGroupEvent(parseUserId(msg));
                break;
        }
    }

    private String parseUserId(String msg) {
        Gson gson = new Gson();
        MsgData data = new MsgData();
        if (msg.contains("@all")) {
            data.addUser("0");
        } else if (msg.contains("@here")) {
            data.addUser("00");
        } else {
            addUsers(msg, data);
        }
        return gson.toJson(data);
    }

    private void addUsers(String msg, MsgData data) {
        Pattern pattern = Pattern.compile("@([\\S]+\\b)");
        Matcher matcher = pattern.matcher(msg);
        while (matcher.find()) {
            String str = matcher.group(1);
            data.addUser(str);
        }
    }

    private class MsgData {
        private List<String> mentions = new ArrayList<>();

        public void addUser(String str) {
            mentions.add(str);
        }
    }

    public void trackRoom(Room room) {
        switch (room.getType()) {
            case Room.TYPE_DIRECT_MESSAGE:
                trackingHelper.directMessageScreen(room.getName());
                break;
            case Room.TYPE_GROUP:
                trackingHelper.groupChatScreen(room.getName());
                break;
        }
    }
}
