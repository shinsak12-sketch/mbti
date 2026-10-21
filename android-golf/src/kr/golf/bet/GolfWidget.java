package kr.golf.bet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.HashMap;

/** 홈 화면 달력 위젯: 이번 달 · 다녀온 날(채운 원) · 예정(테두리 원). 비트맵으로 그려서 ImageView에 붙임. */
public class GolfWidget extends AppWidgetProvider {

  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) update(c, m, id);
  }

  static void updateAll(Context c) {
    try {
      AppWidgetManager m = AppWidgetManager.getInstance(c);
      int[] ids = m.getAppWidgetIds(new ComponentName(c, GolfWidget.class));
      for (int id : ids) update(c, m, id);
    } catch (Exception ignored) {}
  }

  static void update(Context c, AppWidgetManager m, int id) {
    String pkg = c.getPackageName();
    int layout = c.getResources().getIdentifier("golf_widget", "layout", pkg);
    int img = c.getResources().getIdentifier("wimg", "id", pkg);
    RemoteViews v = new RemoteViews(pkg, layout);
    v.setImageViewBitmap(img, draw(c));
    Intent it = new Intent(c, MainActivity.class);
    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pi = PendingIntent.getActivity(c, 0, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    v.setOnClickPendingIntent(img, pi);
    m.updateAppWidget(id, v);
  }

  static Bitmap draw(Context c) {
    String days = c.getSharedPreferences("golf", Context.MODE_PRIVATE).getString("days", "");
    Calendar now = Calendar.getInstance();
    int y = now.get(Calendar.YEAR), mo = now.get(Calendar.MONTH) + 1, today = now.get(Calendar.DAY_OF_MONTH);
    String prefix = y + "-" + (mo < 10 ? "0" : "") + mo + "-";
    HashMap<Integer, Integer> played = new HashMap<>(), plan = new HashMap<>();
    int nPlayed = 0, nPlan = 0;
    for (String part : days.split(",")) {
      if (!part.startsWith(prefix) || part.length() < 12) continue;
      try {
        int d = Integer.parseInt(part.substring(8, 10));
        boolean isPlan = part.endsWith(":1");
        HashMap<Integer, Integer> map = isPlan ? plan : played;
        Integer cur = map.get(d); map.put(d, cur == null ? 1 : cur + 1);
        if (isPlan) nPlan++; else nPlayed++;
      } catch (Exception ignored) {}
    }

    final int W = 720, H = 660;
    Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    Canvas cv = new Canvas(b);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColor(Color.parseColor("#171a20"));
    cv.drawRoundRect(new RectF(0, 0, W, H), 52, 52, p);

    Typeface bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    p.setTypeface(bold);
    p.setColor(Color.WHITE); p.setTextSize(62); p.setTextAlign(Paint.Align.LEFT);
    cv.drawText(mo + "월", 44, 92, p);
    p.setTextSize(28); p.setColor(Color.parseColor("#8b919e"));
    cv.drawText(String.valueOf(y), 44 + p.measureText("") + measure(bold, 62, mo + "월") + 14, 92, p);
    p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(30);
    p.setColor(Color.parseColor("#c6ff3d")); String s1 = "다녀옴 " + nPlayed;
    p.setColor(Color.parseColor("#c6ff3d")); cv.drawText(s1, W - 44 - measure(bold, 30, "  예정 " + nPlan), 88, p);
    p.setColor(Color.parseColor("#5b94ff")); cv.drawText("  예정 " + nPlan, W - 44, 88, p);

    String[] wd = {"일", "월", "화", "수", "목", "금", "토"};
    float left = 30, cw = (W - 60) / 7f, top = 150, ch = 78;
    p.setTextAlign(Paint.Align.CENTER); p.setTextSize(24);
    for (int i = 0; i < 7; i++) {
      p.setColor(i == 0 ? Color.parseColor("#ff7a7a") : Color.parseColor("#8b919e"));
      cv.drawText(wd[i], left + cw * i + cw / 2, top - 18, p);
    }
    Calendar first = Calendar.getInstance(); first.set(y, mo - 1, 1);
    int startDow = first.get(Calendar.DAY_OF_WEEK) - 1;
    int dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
    float r = Math.min(cw, ch) * 0.40f;
    for (int d = 1; d <= dim; d++) {
      int idx = startDow + d - 1, col = idx % 7, row = idx / 7;
      float cx = left + cw * col + cw / 2, cy = top + ch * row + ch / 2;
      boolean hp = played.containsKey(d), hl = plan.containsKey(d);
      if (hp) { p.setStyle(Paint.Style.FILL); p.setColor(Color.parseColor("#c6ff3d")); cv.drawCircle(cx, cy, r, p); }
      if (hl) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5); p.setColor(Color.parseColor("#5b94ff")); cv.drawCircle(cx, cy, hp ? r + 5 : r, p); }
      if (d == today && !hp && !hl) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(Color.parseColor("#f3f4f6")); cv.drawCircle(cx, cy, r, p); }
      p.setStyle(Paint.Style.FILL); p.setTextSize(30);
      int tc = hp ? Color.parseColor("#0e1014") : (hl ? Color.parseColor("#8fb6ff") : (col == 0 ? Color.parseColor("#ff9b9b") : Color.parseColor("#d7d9de")));
      p.setColor(tc);
      cv.drawText(String.valueOf(d), cx, cy + 11, p);
      Integer cnt = played.get(d);
      if (cnt != null && cnt > 1) {
        p.setColor(Color.parseColor("#0e1014")); p.setTextSize(18);
        cv.drawText("×" + cnt, cx + r * 0.62f, cy - r * 0.52f, p);
      }
    }
    return b;
  }

  static float measure(Typeface tf, float size, String s) {
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTypeface(tf); p.setTextSize(size); return p.measureText(s);
  }
}
