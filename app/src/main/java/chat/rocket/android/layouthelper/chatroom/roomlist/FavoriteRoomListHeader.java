package chat.rocket.android.layouthelper.chatroom.roomlist;

import android.support.annotation.NonNull;

import chat.rocket.core.models.RoomSidebar;
import java.util.List;

public class FavoriteRoomListHeader implements RoomListHeader {

  private final String title;

  public FavoriteRoomListHeader(String title) {
    this.title = title;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public boolean owns(RoomSidebar roomSidebar) {
    return roomSidebar.isFavorite();
  }

  @Override
  public boolean shouldShow(@NonNull List<RoomSidebar> roomSidebarList) {
    for (int i = 0, size = roomSidebarList.size(); i < size; i++) {
      if (roomSidebarList.get(i).isFavorite()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ClickListener getClickListener() {
    return null;
  }
}