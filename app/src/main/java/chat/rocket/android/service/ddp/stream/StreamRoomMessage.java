package chat.rocket.android.service.ddp.stream;

import android.content.Context;
import chat.rocket.android.api.DDPClientWraper;
import chat.rocket.android.model.ddp.Message;
import chat.rocket.android.realm_helper.RealmHelper;
import io.realm.RealmObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * stream-room-message subscriber.
 */
public class StreamRoomMessage extends AbstractStreamNotifyEventSubscriber {
  private String roomId;

  public StreamRoomMessage(Context context, RealmHelper realmHelper, DDPClientWraper ddpClient,
      String roomId) {
    super(context, realmHelper, ddpClient);
    this.roomId = roomId;
  }

  @Override protected String getSubscriptionName() {
    return "stream-room-messages";
  }

  @Override protected JSONArray getSubscriptionParams() throws JSONException {
    return new JSONArray()
        .put(roomId)
        .put(false);
  }

  @Override protected Class<? extends RealmObject> getModelClass() {
    return Message.class;
  }

  @Override protected String getPrimaryKeyForModel() {
    return "_id";
  }

  @Override protected JSONObject customizeFieldJson(JSONObject json) throws JSONException {
    return Message.customizeJson(super.customizeFieldJson(json));
  }
}