package kr.golf.bet;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/** 예정 라운드 알림: 전날 정한 시각 · 당일 티오프 N분 전. 데이터가 바뀔 때마다 다시 맞춤. */
public class Reminders {
  static final String CH = "golf_rounds";

  static void ensureChannel(Context c) {
    if (Build.VERSION.SDK_INT < 26) return;
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm.getNotificationChannel(CH) == null) {
      NotificationChannel ch = new NotificationChannel(CH, "라운드 알림", NotificationManager.IMPORTANCE_HIGH);
      ch.setDescription("예정 라운드 전날 · 티오프 전 알림");
      nm.createNotificationChannel(ch);
    }
  }

  static PendingIntent alarmPi(Context c, String key, String title, String text, String id) {
    Intent i = new Intent(c, ReminderReceiver.class);
    i.setData(Uri.parse("golfr://" + key));
    i.putExtra("title", title); i.putExtra("text", text); i.putExtra("id", id);
    return PendingIntent.getBroadcast(c, key.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  static String two(int n) { return (n < 10 ? "0" : "") + n; }

  /** sum = 웹에서 저장한 요약 JSON (records, rem) */
  static void schedule(Context c) {
    try {
      SharedPreferences sp = c.getSharedPreferences("golf", Context.MODE_PRIVATE);
      JSONObject sum = new JSONObject(sp.getString("sum", "{}"));
      AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
      // 이전 알림 취소
      String old = sp.getString("remKeys", "");
      for (String k : old.split("\n")) { if (k.length() == 0) continue; am.cancel(alarmPi(c, k, "", "", "")); }
      StringBuilder keys = new StringBuilder();
      JSONObject rem = sum.optJSONObject("rem"); if (rem == null) rem = new JSONObject();
      boolean eve = rem.optBoolean("eve", true), day = rem.optBoolean("day", true);
      String eveTime = rem.optString("eveTime", "20:00"); int before = rem.optInt("dayBefore", 120);
      JSONArray recs = sum.optJSONArray("records");
      long now = System.currentTimeMillis();
      if (recs != null) for (int i = 0; i < recs.length(); i++) {
        JSONObject r = recs.optJSONObject(i); if (r == null || r.optInt("p") != 1) continue;
        String d = r.optString("d"); if (d.length() != 10) continue;
        int y = Integer.parseInt(d.substring(0, 4)), mo = Integer.parseInt(d.substring(5, 7)), dd = Integer.parseInt(d.substring(8, 10));
        String tee = r.optString("t"), course = r.optString("c"), mates = r.optString("m"), id = r.optString("i");
        String body = mo + "/" + dd + (tee.length() > 0 ? " " + tee : "") + "  " + course + (mates.length() > 0 ? " · " + mates : "");
        if (eve) {
          Calendar k = Calendar.getInstance(); k.clear(); k.set(y, mo - 1, dd); k.add(Calendar.DAY_OF_MONTH, -1);
          try { k.set(Calendar.HOUR_OF_DAY, Integer.parseInt(eveTime.substring(0, 2))); k.set(Calendar.MINUTE, Integer.parseInt(eveTime.substring(3, 5))); } catch (Exception e) { k.set(Calendar.HOUR_OF_DAY, 20); }
          if (k.getTimeInMillis() > now) {
            String key = id + "/eve"; am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, k.getTimeInMillis(), alarmPi(c, key, "내일 라운드", body, id)); keys.append(key).append("\n");
          }
        }
        if (day && tee.length() == 5) {
          Calendar k = Calendar.getInstance(); k.clear(); k.set(y, mo - 1, dd, Integer.parseInt(tee.substring(0, 2)), Integer.parseInt(tee.substring(3, 5)));
          k.add(Calendar.MINUTE, -before);
          if (k.getTimeInMillis() > now) {
            String t = before % 60 == 0 ? (before / 60) + "시간" : before + "분";
            String key = id + "/day"; am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, k.getTimeInMillis(), alarmPi(c, key, t + " 뒤 티오프", body, id)); keys.append(key).append("\n");
          }
        }
      }
      sp.edit().putString("remKeys", keys.toString()).apply();
    } catch (Exception ignored) {}
  }

  /** 동행자 공유 일정 알림 (같은 일정은 한 칸에서 갱신) */
  static void notifyShared(Context c, String title, String text, String id, int nid) { show(c, title, text, id, nid); }

  static void show(Context c, String title, String text, String id) { show(c, title, text, id, (id + title).hashCode()); }

  static void show(Context c, String title, String text, String id, int nid) {
    ensureChannel(c);
    Intent open = new Intent(c, MainActivity.class);
    open.setAction(Intent.ACTION_VIEW);
    open.setData(Uri.parse("golf://w?a=open&d=&id=" + id + "&r=n"));
    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi = PendingIntent.getActivity(c, nid, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
    b.setSmallIcon(c.getResources().getIdentifier("ic_stat_golf", "drawable", c.getPackageName()))
     .setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
     .setAutoCancel(true).setContentIntent(pi).setColor(0xFF6FA516);
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(nid, b.build());
  }
}
