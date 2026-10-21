package kr.golf.bet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 알람 시각이 되면 알림 띄움 */
public class ReminderReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
    String t = i.getStringExtra("title"), x = i.getStringExtra("text"), id = i.getStringExtra("id");
    if (t == null || t.length() == 0) return;
    Reminders.show(c, t, x == null ? "" : x, id == null ? "" : id);
  }
}
