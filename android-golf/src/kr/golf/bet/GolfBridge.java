package kr.golf.bet;

import android.content.Context;
import android.webkit.JavascriptInterface;

/** 웹 → 네이티브: 위젯에 쓸 요약(올해 누적, 날짜별 기록·예정, 다가오는 예정)을 저장하고 위젯을 새로 그림. */
public class GolfBridge {
  private final Context ctx;
  GolfBridge(Context c) { ctx = c.getApplicationContext(); }

  @JavascriptInterface public void saveSummary(String json) {
    ctx.getSharedPreferences("golf", Context.MODE_PRIVATE).edit().putString("sum", json == null ? "{}" : json).apply();
    GolfWidget.updateAll(ctx);
  }
}
