package kr.golf.bet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 알람 시각이 되면 날씨를 붙여서 알림 띄움 (날씨 못 받으면 그냥 띄움) */
public class ReminderReceiver extends BroadcastReceiver {
  @Override public void onReceive(final Context c, Intent i) {
    final String t = i.getStringExtra("title"), x = i.getStringExtra("text"), id = i.getStringExtra("id");
    final String d = i.getStringExtra("d"), tee = i.getStringExtra("t");
    final double la = i.getDoubleExtra("la", 999), lo = i.getDoubleExtra("lo", 999);
    if (t == null || t.length() == 0) return;
    final PendingResult pr = goAsync();
    new Thread(new Runnable() { @Override public void run() {
      try {
        String[] w = (la != 999 && d != null) ? Weather.line(la, lo, d, tee) : null;
        String title = w != null && "1".equals(w[1]) ? t + " · 비 예보" : t;
        String body = (x == null ? "" : x) + (w != null ? "\n" + w[0] : "");
        Reminders.show(c.getApplicationContext(), title, body, id == null ? "" : id);
      } finally { pr.finish(); }
    } }).start();
  }
}
