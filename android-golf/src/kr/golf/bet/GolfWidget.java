package kr.golf.bet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 홈 화면 골프 위젯 (4x7)
 *  - 올해 누적 · [+ 예정] [+ 기록] → 홈 화면 위 팝업에서 입력
 *  - 다음 라운드 한 줄 → 팝업 메뉴
 *  - 달력: ‹ › 로 위젯 안에서 달 넘김, 월 이름 누르면 이번 달, 날짜 누르면 선택(리스트 필터) · 다시 누르면 해제
 *  - 리스트: 보고 있는 달(또는 선택한 날)의 라운드 · 줄 누르면 팝업 메뉴(수정 · 다녀옴 · 삭제)
 */
public class GolfWidget extends AppWidgetProvider {
  static final String A_PREV = "kr.golf.bet.W_PREV", A_NEXT = "kr.golf.bet.W_NEXT", A_TODAY = "kr.golf.bet.W_TODAY", A_SEL = "kr.golf.bet.W_SEL";
  static final String[] DOW = {"일", "월", "화", "수", "목", "금", "토"};

  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) update(c, m, id); }

  @Override public void onReceive(Context c, Intent i) {
    super.onReceive(c, i);
    String act = i.getAction();
    if (act == null || !act.startsWith("kr.golf.bet.W_")) return;
    int wid = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    if (wid < 0) return;
    SharedPreferences sp = prefs(c);
    int[] ym = ym(c, wid); int sel = sp.getInt("sel_" + wid, 0);
    if (A_PREV.equals(act)) { ym[1]--; if (ym[1] < 1) { ym[1] = 12; ym[0]--; } sel = 0; }
    else if (A_NEXT.equals(act)) { ym[1]++; if (ym[1] > 12) { ym[1] = 1; ym[0]++; } sel = 0; }
    else if (A_TODAY.equals(act)) { Calendar n = Calendar.getInstance(); ym[0] = n.get(Calendar.YEAR); ym[1] = n.get(Calendar.MONTH) + 1; sel = 0; }
    else if (A_SEL.equals(act)) { int d = i.getIntExtra("day", 0); sel = (sel == d) ? 0 : d; }
    sp.edit().putString("ym_" + wid, ym[0] + "-" + two(ym[1])).putInt("sel_" + wid, sel).commit();
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    update(c, m, wid);
    m.notifyAppWidgetViewDataChanged(wid, rid(c, "wlist", "id"));
  }

  @Override public void onDeleted(Context c, int[] ids) {
    SharedPreferences.Editor e = prefs(c).edit();
    for (int id : ids) { e.remove("ym_" + id); e.remove("sel_" + id); }
    e.apply();
  }

  /** 데이터가 바뀌었을 때 모든 위젯 다시 그림 */
  static void refreshAll(Context c) {
    try {
      AppWidgetManager m = AppWidgetManager.getInstance(c);
      int[] ids = m.getAppWidgetIds(new ComponentName(c, GolfWidget.class));
      for (int id : ids) update(c, m, id);
      if (ids.length > 0) m.notifyAppWidgetViewDataChanged(ids, rid(c, "wlist", "id"));
    } catch (Exception ignored) {}
  }

  static SharedPreferences prefs(Context c) { return c.getSharedPreferences("golf", Context.MODE_PRIVATE); }
  static int rid(Context c, String name, String type) { return c.getResources().getIdentifier(name, type, c.getPackageName()); }
  static String two(int n) { return (n < 10 ? "0" : "") + n; }
  static String todayStr() { Calendar n = Calendar.getInstance(); return n.get(Calendar.YEAR) + "-" + two(n.get(Calendar.MONTH) + 1) + "-" + two(n.get(Calendar.DAY_OF_MONTH)); }

  static int[] ym(Context c, int wid) {
    String s = prefs(c).getString("ym_" + wid, null);
    Calendar n = Calendar.getInstance();
    int[] r = {n.get(Calendar.YEAR), n.get(Calendar.MONTH) + 1};
    if (s != null && s.length() == 7) { try { r[0] = Integer.parseInt(s.substring(0, 4)); r[1] = Integer.parseInt(s.substring(5, 7)); } catch (Exception ignored) {} }
    return r;
  }

  static long dayNum(String ds) {
    try {
      Calendar k = Calendar.getInstance(); k.clear();
      k.set(Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(5, 7)) - 1, Integer.parseInt(ds.substring(8, 10)));
      return Math.round(k.getTimeInMillis() / 86400000.0);
    } catch (Exception e) { return Long.MIN_VALUE; }
  }
  static String dow(String ds) {
    try { Calendar k = Calendar.getInstance(); k.clear(); k.set(Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(5, 7)) - 1, Integer.parseInt(ds.substring(8, 10))); return DOW[k.get(Calendar.DAY_OF_WEEK) - 1]; }
    catch (Exception e) { return ""; }
  }

  static List<JSONObject> records(Context c) {
    List<JSONObject> out = new ArrayList<>();
    try {
      JSONArray a = new JSONObject(prefs(c).getString("sum", "{}")).optJSONArray("records");
      if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null && o.optString("d").length() == 10) out.add(o); }
    } catch (Exception ignored) {}
    return out;
  }

  static final Comparator<JSONObject> BY_DATE = new Comparator<JSONObject>() {
    @Override public int compare(JSONObject a, JSONObject b) {
      int x = a.optString("d").compareTo(b.optString("d"));
      return x != 0 ? x : Long.compare(a.optLong("o"), b.optLong("o"));
    }
  };

  /** 리스트에 보일 줄: 보고 있는 달 (선택한 날이 있으면 그 날만) */
  static List<JSONObject> rowsFor(Context c, int wid) {
    int[] ym = ym(c, wid); int sel = prefs(c).getInt("sel_" + wid, 0);
    String pre = ym[0] + "-" + two(ym[1]) + "-"; String day = sel > 0 ? pre + two(sel) : null;
    List<JSONObject> out = new ArrayList<>();
    for (JSONObject o : records(c)) { String d = o.optString("d"); if (day != null ? d.equals(day) : d.startsWith(pre)) out.add(o); }
    Collections.sort(out, Collections.reverseOrder(BY_DATE));
    return out;
  }

  static PendingIntent bc(Context c, int wid, String act, int day) {
    Intent i = new Intent(c, GolfWidget.class);
    i.setAction(act);
    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
    i.putExtra("day", day);
    i.setData(Uri.parse("golfw://" + wid + "/" + act + "/" + day));
    return PendingIntent.getBroadcast(c, wid * 1000 + day * 10 + act.length() % 10, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }
  static PendingIntent quick(Context c, String a, String d, String id, int req) {
    Intent i = new Intent(c, QuickActivity.class);
    i.setData(Uri.parse("golfq://q?a=" + a + "&d=" + d + "&id=" + id + "&r=" + req));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }
  static PendingIntent app(Context c, String a, int req) {
    Intent i = new Intent(c, MainActivity.class);
    i.setAction(Intent.ACTION_VIEW);
    i.setData(Uri.parse("golf://w?a=" + a + "&d=&id=&r=" + req));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  static void update(Context c, AppWidgetManager m, int wid) {
    String pkg = c.getPackageName();
    RemoteViews v = new RemoteViews(pkg, rid(c, "golf_widget", "layout"));
    List<JSONObject> all = records(c);
    Calendar now = Calendar.getInstance();
    int ty = now.get(Calendar.YEAR); String today = todayStr(); long todayN = dayNum(today);

    // 올해 누적
    int n = 0, h = 0; JSONObject todayPlan = null, next = null;
    List<JSONObject> sorted = new ArrayList<>(all); Collections.sort(sorted, BY_DATE);
    for (JSONObject o : sorted) {
      String d = o.optString("d"); boolean plan = o.optInt("p") == 1;
      if (!plan && d.startsWith(ty + "-")) { n++; h += o.optInt("h", 18); }
      if (plan) { long dn = dayNum(d); if (dn == todayN && todayPlan == null) todayPlan = o; if (dn >= todayN && next == null) next = o; }
    }
    v.setTextViewText(rid(c, "wyear", "id"), ty + "년 골프");
    v.setTextViewText(rid(c, "wstat", "id"), n + "회 · " + String.format("%,d", h) + "홀");
    v.setOnClickPendingIntent(rid(c, "wyearbox", "id"), app(c, "stats", 11));

    // 버튼 (홈 화면 위 팝업)
    int[] ym = ym(c, wid); int sel = prefs(c).getInt("sel_" + wid, 0);
    String selDate = sel > 0 ? ym[0] + "-" + two(ym[1]) + "-" + two(sel) : "";
    if (todayPlan != null) v.setOnClickPendingIntent(rid(c, "wadd", "id"), quick(c, "done", "", todayPlan.optString("i"), wid * 10 + 1));
    else v.setOnClickPendingIntent(rid(c, "wadd", "id"), quick(c, "add", (sel > 0 && dayNum(selDate) <= todayN) ? selDate : today, "", wid * 10 + 1));
    v.setOnClickPendingIntent(rid(c, "wplan", "id"), quick(c, "plan", (sel > 0 && dayNum(selDate) > todayN) ? selDate : "", "", wid * 10 + 2));

    // 다음 라운드 한 줄
    int nn = rid(c, "wnextname", "id"), nd = rid(c, "wnextd", "id"), nb = rid(c, "wnext", "id");
    if (next != null) {
      String d = next.optString("d"); long dd = dayNum(d) - todayN;
      String who = next.optString("m");
      String tee = next.optString("t");
      v.setTextViewText(nn, Integer.parseInt(d.substring(5, 7)) + "/" + Integer.parseInt(d.substring(8, 10)) + " (" + dow(d) + ")" + (tee.length() > 0 ? " " + tee : "") + "  " + next.optString("c") + (who.length() > 0 ? " · " + who : ""));
      v.setTextViewText(nd, dd == 0 ? "오늘" : dd == 1 ? "내일" : "D-" + dd);
      v.setOnClickPendingIntent(nb, quick(c, dd == 0 ? "done" : "menu", "", next.optString("i"), wid * 10 + 3));
    } else {
      v.setTextViewText(nn, "다음 예정 없음");
      v.setTextViewText(nd, "+");
      v.setOnClickPendingIntent(nb, quick(c, "plan", "", "", wid * 10 + 3));
    }

    // 달력
    String pre = ym[0] + "-" + two(ym[1]) + "-";
    HashMap<Integer, Integer> played = new HashMap<>(), plan = new HashMap<>(); int nP = 0, nL = 0;
    for (JSONObject o : all) {
      String d = o.optString("d"); if (!d.startsWith(pre)) continue;
      int day = Integer.parseInt(d.substring(8, 10)); boolean isPlan = o.optInt("p") == 1;
      HashMap<Integer, Integer> mp = isPlan ? plan : played; Integer cur = mp.get(day); mp.put(day, cur == null ? 1 : cur + 1);
      if (isPlan) nL++; else nP++;
    }
    boolean isCur = ym[0] == ty && ym[1] == now.get(Calendar.MONTH) + 1; int td = now.get(Calendar.DAY_OF_MONTH);
    v.setTextViewText(rid(c, "wmonth", "id"), ym[0] + "년 " + ym[1] + "월");
    v.setTextViewText(rid(c, "wmsum", "id"), "다녀옴 " + nP + " · 예정 " + nL);
    v.setOnClickPendingIntent(rid(c, "wprev", "id"), bc(c, wid, A_PREV, 0));
    v.setOnClickPendingIntent(rid(c, "wnextm", "id"), bc(c, wid, A_NEXT, 0));
    v.setOnClickPendingIntent(rid(c, "wmonth", "id"), bc(c, wid, A_TODAY, 0));
    Calendar first = Calendar.getInstance(); first.clear(); first.set(ym[0], ym[1] - 1, 1);
    int start = first.get(Calendar.DAY_OF_WEEK) - 1, dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
    int rows = (start + dim + 6) / 7;
    for (int r = 0; r < 6; r++) v.setViewVisibility(rid(c, "r" + r, "id"), r < rows ? View.VISIBLE : View.GONE);
    int dNone = rid(c, "c_none", "drawable"), dFill = rid(c, "c_fill", "drawable"), dRing = rid(c, "c_ring", "drawable"), dBoth = rid(c, "c_both", "drawable"),
        dToday = rid(c, "c_today", "drawable"), dSel = rid(c, "c_sel", "drawable"), dFillSel = rid(c, "c_fill_sel", "drawable");
    for (int i = 0; i < 42; i++) {
      int cell = rid(c, "d" + i, "id"); int d = i - start + 1;
      if (d < 1 || d > dim) { v.setTextViewText(cell, ""); v.setInt(cell, "setBackgroundResource", dNone); continue; }
      boolean hp = played.containsKey(d), hl = plan.containsKey(d), hs = d == sel; int col = i % 7;
      v.setTextViewText(cell, String.valueOf(d));
      int bg = hs ? (hp ? dFillSel : dSel) : hp && hl ? dBoth : hp ? dFill : hl ? dRing : (isCur && d == td) ? dToday : dNone;
      v.setInt(cell, "setBackgroundResource", bg);
      v.setTextColor(cell, Color.parseColor(hp ? "#0e1014" : hl ? "#8fb6ff" : col == 0 ? "#ff8a8a" : "#d7d9de"));
      v.setOnClickPendingIntent(cell, bc(c, wid, A_SEL, d));
    }

    // 리스트
    int cnt = rowsFor(c, wid).size();
    v.setTextViewText(rid(c, "wlisthead", "id"), sel > 0
        ? ym[1] + "/" + sel + " (" + dow(selDate) + ") " + cnt + "개 · 날짜 다시 누르면 " + ym[1] + "월 전체"
        : ym[1] + "월 라운드 " + cnt + "개");
    v.setTextViewText(rid(c, "wempty", "id"), all.isEmpty() ? "앱을 한 번 열면 기록이 들어와" : sel > 0 ? "이 날 기록 없음 · 위 + 버튼으로 추가" : "이 달 기록 없음");
    Intent svc = new Intent(c, GolfListService.class);
    svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
    svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
    int list = rid(c, "wlist", "id");
    v.setRemoteAdapter(list, svc);
    v.setEmptyView(list, rid(c, "wempty", "id"));
    Intent tpl = new Intent(c, QuickActivity.class);
    tpl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    v.setPendingIntentTemplate(list, PendingIntent.getActivity(c, 90000 + wid, tpl, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

    m.updateAppWidget(wid, v);
  }
}
