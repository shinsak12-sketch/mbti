package kr.golf.bet;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** 위젯 리스트: 보고 있는 달(또는 선택한 날)의 라운드 줄 */
public class GolfListService extends RemoteViewsService {
  @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new Factory(getApplicationContext(), intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
  }

  static class Factory implements RemoteViewsFactory {
    final Context c; final int wid; List<JSONObject> rows = new ArrayList<>();
    Factory(Context c, int wid) { this.c = c; this.wid = wid; }
    @Override public void onCreate() {}
    @Override public void onDataSetChanged() { rows = GolfWidget.rowsFor(c, wid); }
    @Override public void onDestroy() {}
    @Override public int getCount() { return rows.size(); }
    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int p) { return p; }
    @Override public boolean hasStableIds() { return false; }

    @Override public RemoteViews getViewAt(int pos) {
      RemoteViews v = new RemoteViews(c.getPackageName(), GolfWidget.rid(c, "golf_widget_row", "layout"));
      if (pos < 0 || pos >= rows.size()) return v;
      JSONObject r = rows.get(pos);
      String d = r.optString("d"); boolean plan = r.optInt("p") == 1; int holes = r.optInt("h", 18), score = r.optInt("s");
      v.setTextViewText(id("rday"), String.valueOf(Integer.parseInt(d.substring(8, 10))));
      v.setTextViewText(id("rdow"), Integer.parseInt(d.substring(5, 7)) + "월 " + GolfWidget.dow(d));
      v.setTextViewText(id("rname"), r.optString("c"));
      StringBuilder sub = new StringBuilder();
      String tee = r.optString("t"); if (tee.length() > 0) add(sub, tee + " 티");
      add(sub, r.optString("g")); add(sub, r.optString("m")); add(sub, r.optInt("k") == 1 ? "캐디" : "노캐디"); if (holes != 18) add(sub, holes + "홀");
      v.setTextViewText(id("rsub"), sub.toString());
      v.setInt(id("rrow"), "setBackgroundResource", GolfWidget.rid(c, plan ? "row_plan" : "row_bg", "drawable"));
      int dayC = Color.parseColor(plan ? "#8fb6ff" : "#f3f4f6");
      v.setTextColor(id("rday"), dayC);
      long ddAll = GolfWidget.dayNum(d) - GolfWidget.dayNum(GolfWidget.todayStr());
      boolean needScore = (plan && ddAll < 0) || (!plan && score <= 0);
      v.setInt(id("rright"), "setBackgroundResource", GolfWidget.rid(c, needScore ? "w_btn_score" : "c_none", "drawable"));
      if (needScore) {
        v.setTextViewText(id("rright"), "+ 타수");
        v.setTextColor(id("rright"), Color.parseColor("#c6ff3d"));
        v.setTextViewText(id("rright2"), plan ? "지난 예정" : "");
        Intent sf = new Intent(); sf.putExtra("a", "score"); sf.putExtra("id", r.optString("i")); sf.putExtra("d", d);
        v.setOnClickFillInIntent(id("rright"), sf);
      } else if (plan) {
        long dd = ddAll;
        v.setTextViewText(id("rright"), dd < 0 ? "지남" : dd == 0 ? "오늘" : dd == 1 ? "내일" : "D-" + dd);
        v.setTextColor(id("rright"), Color.parseColor("#5b94ff"));
        v.setTextViewText(id("rright2"), "예정");
      } else {
        v.setTextViewText(id("rright"), score > 0 ? String.valueOf(score) : "—");
        v.setTextColor(id("rright"), Color.parseColor(score > 0 ? "#f3f4f6" : "#8b919e"));
        int diff = score - 72;
        v.setTextViewText(id("rright2"), score > 0 ? (diff > 0 ? "+" + diff : String.valueOf(diff)) : "");
      }
      Intent fill = new Intent();
      fill.putExtra("a", "menu"); fill.putExtra("id", r.optString("i")); fill.putExtra("d", d);
      v.setOnClickFillInIntent(id("rrow"), fill);
      return v;
    }
    int id(String n) { return GolfWidget.rid(c, n, "id"); }
    static void add(StringBuilder sb, String s) { if (s == null || s.length() == 0) return; if (sb.length() > 0) sb.append(" · "); sb.append(s); }
  }
}
