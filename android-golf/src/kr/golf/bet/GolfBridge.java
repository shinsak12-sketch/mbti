package kr.golf.bet;

import android.content.Context;
import android.webkit.JavascriptInterface;

/** 웹 → 네이티브: 라운드 날짜 목록을 저장하고 홈 화면 달력 위젯을 새로 그림. */
public class GolfBridge {
  private final Context ctx;
  GolfBridge(Context c) { ctx = c.getApplicationContext(); }

  /** days = "2026-10-17:0,2026-10-25:1" (0 다녀옴, 1 예정) */
  @JavascriptInterface public void saveDays(String days) {
    ctx.getSharedPreferences("golf", Context.MODE_PRIVATE).edit().putString("days", days == null ? "" : days).apply();
    GolfWidget.updateAll(ctx);
  }
}
