package kr.golf.bet;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;

/** 웹 → 네이티브: 위젯용 기록 요약 저장 · 위젯 갱신 · 팝업 닫기 */
public class GolfBridge {
  private final Context ctx;
  private final Activity act;
  GolfBridge(Activity a) { ctx = a.getApplicationContext(); act = a; }

  @JavascriptInterface public void saveSummary(String json) {
    ctx.getSharedPreferences("golf", Context.MODE_PRIVATE).edit().putString("sum", json == null ? "{}" : json).commit();
    GolfWidget.refreshAll(ctx);
  }
  @JavascriptInterface public void close() {
    if (act instanceof QuickActivity) act.runOnUiThread(new Runnable() { @Override public void run() { act.finish(); } });
  }
}
