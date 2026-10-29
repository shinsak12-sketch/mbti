package kr.golf.bet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.DatePicker;

/** 웹 → 네이티브: 위젯용 기록 요약 저장 · 위젯 갱신 · 팝업 닫기 · 폰 기본 날짜 선택 창 */
public class GolfBridge {
  private final Context ctx;
  private final Activity act;
  private final WebView wv;
  GolfBridge(Activity a, WebView w) { ctx = a.getApplicationContext(); act = a; wv = w; }

  @JavascriptInterface public void saveSummary(String json) {
    ctx.getSharedPreferences("golf", Context.MODE_PRIVATE).edit().putString("sum", json == null ? "{}" : json).commit();
    GolfWidget.refreshAll(ctx);
    Reminders.schedule(ctx);
    if (json != null && json.contains("\"sync\"")) SyncJob.ensure(ctx);
  }

  /** 백그라운드 확인이 이미 본 공유 일정 표시 (웹이 받은함을 합친 뒤 알림 중복 막기) */
  @JavascriptInterface public void markSeen(String json) {
    ctx.getSharedPreferences("golf", Context.MODE_PRIVATE).edit().putString("nseen", json == null ? "{}" : json).apply();
  }

  /** 정산 보고서 이미지 보내기: PNG(base64) → 앱 캐시에 저장 → 공유 창 */
  @JavascriptInterface public void shareImage(final String b64, final String text) {
    try {
      byte[] png = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
      java.io.File d = new java.io.File(ctx.getCacheDir(), "cam"); d.mkdirs();
      java.io.File[] old = d.listFiles(); if (old != null) for (java.io.File f : old) if (f.getName().startsWith("share_")) f.delete();
      final String name = "share_" + System.currentTimeMillis() + ".png";
      java.io.FileOutputStream o = new java.io.FileOutputStream(new java.io.File(d, name)); o.write(png); o.close();
      act.runOnUiThread(new Runnable() { @Override public void run() {
        android.net.Uri u = android.net.Uri.parse("content://kr.golf.bet.photos/" + name);
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
        i.setType("image/png"); i.putExtra(android.content.Intent.EXTRA_STREAM, u);
        if (text != null && text.length() > 0) i.putExtra(android.content.Intent.EXTRA_TEXT, text);
        i.setClipData(android.content.ClipData.newRawUri("정산", u));
        i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { act.startActivity(android.content.Intent.createChooser(i, "정산 보내기")); } catch (Exception ignored) {}
      } });
    } catch (Exception ignored) {}
  }

  /** 친구 코드 보내기: 카톡 등 공유 창 */
  @JavascriptInterface public void shareText(final String text) {
    act.runOnUiThread(new Runnable() { @Override public void run() {
      android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
      i.setType("text/plain"); i.putExtra(android.content.Intent.EXTRA_TEXT, text);
      try { act.startActivity(android.content.Intent.createChooser(i, "코드 보내기")); } catch (Exception ignored) {}
    } });
  }

  /** 알림 권한: "granted" · "ask" (안드로이드 13+에서 아직 안 물어봄/거절) */
  @JavascriptInterface public String notifyState() {
    if (android.os.Build.VERSION.SDK_INT < 33) return "granted";
    return act.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED ? "granted" : "ask";
  }
  @JavascriptInterface public void requestNotify() {
    if (android.os.Build.VERSION.SDK_INT < 33) return;
    act.runOnUiThread(new Runnable() { @Override public void run() { act.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 77); } });
  }
  @JavascriptInterface public void testNotify() { Reminders.show(ctx, "알림 테스트", "라운드 알림이 이렇게 떠", ""); }

  @JavascriptInterface public void close() {
    if (act instanceof QuickActivity) act.runOnUiThread(new Runnable() { @Override public void run() { act.finish(); } });
  }

  /** cur = "2026-11-06" · 고르면 window.onNativeDate("2026-11-06") 호출 */
  @JavascriptInterface public void pickDate(final String cur) {
    act.runOnUiThread(new Runnable() { @Override public void run() {
      int y, m, d;
      try { y = Integer.parseInt(cur.substring(0, 4)); m = Integer.parseInt(cur.substring(5, 7)) - 1; d = Integer.parseInt(cur.substring(8, 10)); }
      catch (Exception e) { java.util.Calendar c = java.util.Calendar.getInstance(); y = c.get(java.util.Calendar.YEAR); m = c.get(java.util.Calendar.MONTH); d = c.get(java.util.Calendar.DAY_OF_MONTH); }
      DatePickerDialog dlg = new DatePickerDialog(act, android.R.style.Theme_DeviceDefault_Dialog_Alert, new DatePickerDialog.OnDateSetListener() {
        @Override public void onDateSet(DatePicker view, int yy, int mm, int dd) {
          final String ds = yy + "-" + (mm + 1 < 10 ? "0" : "") + (mm + 1) + "-" + (dd < 10 ? "0" : "") + dd;
          wv.evaluateJavascript("window.onNativeDate&&window.onNativeDate('" + ds + "')", null);
        }
      }, y, m, d);
      dlg.show();
    } });
  }
}
