package kr.golf.bet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 홈 화면 골프 위젯
 *  - 위: 올해 누적(회·홀) · [+ 오늘 기록] 버튼 (오늘 예정이 있으면 그 예정을 다녀옴으로 전환)
 *  - 가운데: 다음 라운드 카드 (D-day · 골프장 · 동행자) · 없으면 [+ 예정]
 *  - 아래: 이번 달 달력 · 다녀옴(채운 원) 예정(테두리) · 날짜 누르면 앱에서 그 날 열기
 */
public class GolfWidget extends AppWidgetProvider {
  static final String[] DOW = {"일", "월", "화", "수", "목", "금", "토"};

  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) update(c, m, id); }

  static void updateAll(Context c) {
    try {
      AppWidgetManager m = AppWidgetManager.getInstance(c);
      int[] ids = m.getAppWidgetIds(new ComponentName(c, GolfWidget.class));
      for (int id : ids) update(c, m, id);
    } catch (Exception ignored) {}
  }

  static int rid(Context c, String name, String type) { return c.getResources().getIdentifier(name, type, c.getPackageName()); }
  static String two(int n) { return (n < 10 ? "0" : "") + n; }

  static PendingIntent pi(Context c, String a, String d, String id, int req) {
    Intent it = new Intent(c, MainActivity.class);
    it.setAction(Intent.ACTION_VIEW);
    it.setData(Uri.parse("golf://w?a=" + a + "&d=" + d + "&id=" + id + "&r=" + req));
    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(c, req, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  static long dayNum(String ds) {
    try {
      Calendar k = Calendar.getInstance(); k.clear();
      k.set(Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(5, 7)) - 1, Integer.parseInt(ds.substring(8, 10)));
      return k.getTimeInMillis() / 86400000L;
    } catch (Exception e) { return Long.MIN_VALUE; }
  }

  static void update(Context c, AppWidgetManager m, int wid) {
    JSONObject o;
    try { o = new JSONObject(c.getSharedPreferences("golf", Context.MODE_PRIVATE).getString("sum", "{}")); } catch (Exception e) { o = new JSONObject(); }
    RemoteViews v = new RemoteViews(c.getPackageName(), rid(c, "golf_widget", "layout"));
    Calendar now = Calendar.getInstance();
    int y = now.get(Calendar.YEAR), mo = now.get(Calendar.MONTH) + 1, td = now.get(Calendar.DAY_OF_MONTH);
    String today = y + "-" + two(mo) + "-" + two(td);
    long todayN = dayNum(today);

    // 올해 누적
    int n = o.optInt("y") == y ? o.optInt("n") : 0, h = o.optInt("y") == y ? o.optInt("h") : 0;
    v.setTextViewText(rid(c, "wyear", "id"), y + "년 골프");
    v.setTextViewText(rid(c, "wstat", "id"), n + "회 · " + String.format("%,d", h) + "홀");
    v.setOnClickPendingIntent(rid(c, "wyearbox", "id"), pi(c, "stats", "", "", 1));

    // 다음 라운드
    JSONObject todayPlan = null, next = null;
    JSONArray plans = o.optJSONArray("plans");
    if (plans != null) for (int i = 0; i < plans.length(); i++) {
      JSONObject p = plans.optJSONObject(i); if (p == null) continue;
      long dn = dayNum(p.optString("date"));
      if (dn == todayN && todayPlan == null) todayPlan = p;
      if (dn >= todayN && next == null) next = p;
    }
    int tTitle = rid(c, "wnexttitle", "id"), tName = rid(c, "wnextname", "id"), tD = rid(c, "wnextd", "id"), box = rid(c, "wnext", "id");
    if (todayPlan != null) {
      v.setTextViewText(tTitle, "오늘 라운드 · 다녀와서 눌러 기록");
      v.setTextViewText(tName, label(todayPlan));
      v.setTextViewText(tD, "기록 ›"); v.setTextColor(tD, Color.parseColor("#c6ff3d"));
      v.setOnClickPendingIntent(box, pi(c, "done", "", todayPlan.optString("id"), 2));
    } else if (next != null) {
      String ds = next.optString("date");
      Calendar k = Calendar.getInstance(); k.clear();
      k.set(Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(5, 7)) - 1, Integer.parseInt(ds.substring(8, 10)));
      long dd = dayNum(ds) - todayN;
      v.setTextViewText(tTitle, "다음 라운드 · " + Integer.parseInt(ds.substring(5, 7)) + "/" + Integer.parseInt(ds.substring(8, 10)) + " (" + DOW[k.get(Calendar.DAY_OF_WEEK) - 1] + ")");
      v.setTextViewText(tName, label(next));
      v.setTextViewText(tD, dd == 1 ? "내일" : "D-" + dd); v.setTextColor(tD, Color.parseColor("#5b94ff"));
      v.setOnClickPendingIntent(box, pi(c, "open", "", next.optString("id"), 2));
    } else {
      v.setTextViewText(tTitle, "다음 라운드");
      v.setTextViewText(tName, "예정 없음 · 눌러서 추가");
      v.setTextViewText(tD, "+ 예정"); v.setTextColor(tD, Color.parseColor("#5b94ff"));
      v.setOnClickPendingIntent(box, pi(c, "plan", "", "", 2));
    }

    // + 오늘 기록 (오늘 예정이 있으면 그걸 다녀옴으로)
    int add = rid(c, "wadd", "id");
    v.setOnClickPendingIntent(add, todayPlan != null ? pi(c, "done", "", todayPlan.optString("id"), 3) : pi(c, "add", today, "", 3));

    // 이번 달 달력
    String prefix = y + "-" + two(mo) + "-";
    HashMap<Integer, Integer> played = new HashMap<>(), plan = new HashMap<>();
    int nP = 0, nL = 0;
    JSONArray days = o.optJSONArray("days");
    if (days != null) for (int i = 0; i < days.length(); i++) {
      String s = days.optString(i);
      if (!s.startsWith(prefix) || s.length() < 12) continue;
      try {
        int d = Integer.parseInt(s.substring(8, 10)); boolean isPlan = s.endsWith(":1");
        HashMap<Integer, Integer> mp = isPlan ? plan : played; Integer cur = mp.get(d); mp.put(d, cur == null ? 1 : cur + 1);
        if (isPlan) nL++; else nP++;
      } catch (Exception ignored) {}
    }
    v.setTextViewText(rid(c, "wmonth", "id"), mo + "월");
    v.setTextViewText(rid(c, "wmsum", "id"), "다녀옴 " + nP + " · 예정 " + nL);
    Calendar first = Calendar.getInstance(); first.clear(); first.set(y, mo - 1, 1);
    int start = first.get(Calendar.DAY_OF_WEEK) - 1, dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
    int rows = (start + dim + 6) / 7;
    for (int r = 0; r < 6; r++) v.setViewVisibility(rid(c, "r" + r, "id"), r < rows ? View.VISIBLE : View.GONE);
    int dNone = rid(c, "c_none", "drawable"), dFill = rid(c, "c_fill", "drawable"), dRing = rid(c, "c_ring", "drawable"), dBoth = rid(c, "c_both", "drawable"), dToday = rid(c, "c_today", "drawable");
    for (int i = 0; i < 42; i++) {
      int cell = rid(c, "d" + i, "id"); int d = i - start + 1;
      if (d < 1 || d > dim) { v.setTextViewText(cell, ""); v.setInt(cell, "setBackgroundResource", dNone); continue; }
      boolean hp = played.containsKey(d), hl = plan.containsKey(d); int col = i % 7;
      v.setTextViewText(cell, String.valueOf(d));
      v.setInt(cell, "setBackgroundResource", hp && hl ? dBoth : hp ? dFill : hl ? dRing : d == td ? dToday : dNone);
      v.setTextColor(cell, Color.parseColor(hp ? "#0e1014" : hl ? "#8fb6ff" : col == 0 ? "#ff8a8a" : "#d7d9de"));
      v.setOnClickPendingIntent(cell, pi(c, "day", prefix + two(d), "", 100 + d));
    }
    v.setOnClickPendingIntent(rid(c, "wroot", "id"), pi(c, "home", "", "", 4));
    m.updateAppWidget(wid, v);
  }

  static String label(JSONObject p) {
    String s = p.optString("course"); String mt = p.optString("mates");
    return mt.length() > 0 ? s + " · " + mt : s;
  }
}
