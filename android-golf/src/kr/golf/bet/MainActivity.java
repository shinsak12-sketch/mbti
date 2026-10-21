package kr.golf.bet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/** 골프 내기: 로컬 assets/www 를 https://app.local 로 서빙하는 WebView 껍데기. */
public class MainActivity extends Activity {
  private WebView wv;
  private GolfChrome chrome;
  private boolean loaded = false;
  private String pendingJs = null;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setStatusBarColor(Color.parseColor("#0e1014"));
    getWindow().setNavigationBarColor(Color.parseColor("#0e1014"));

    wv = new WebView(this);
    WebSettings s = wv.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(true);
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    wv.setBackgroundColor(Color.parseColor("#0e1014"));
    wv.setOverScrollMode(View.OVER_SCROLL_NEVER);

    wv.setWebViewClient(new AppWebClient(this, new Runnable() { @Override public void run() {
      loaded = true;
      if (pendingJs != null) { wv.evaluateJavascript(pendingJs, null); pendingJs = null; }
    } }));
    chrome = new GolfChrome(this);
    wv.setWebChromeClient(chrome);
    wv.addJavascriptInterface(new GolfBridge(this, wv), "GolfNative");
    setContentView(wv);
    wv.loadUrl("https://app.local/index.html");
    handleIntent(getIntent());
  }

  @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }

  /** 위젯에서 누른 버튼/날짜 → 웹 화면의 해당 기능으로 바로 이동 */
  private void handleIntent(Intent i) {
    Uri u = i == null ? null : i.getData();
    if (u == null || !"golf".equals(u.getScheme())) return;
    String js = "window.widgetOpen&&window.widgetOpen('" + safe(u.getQueryParameter("a")) + "','" + safe(u.getQueryParameter("d")) + "','" + safe(u.getQueryParameter("id")) + "')";
    if (loaded && wv != null) wv.evaluateJavascript(js, null); else pendingJs = js;
  }
  static String safe(String s) { return s == null ? "" : s.replaceAll("[^A-Za-z0-9\\-]", ""); }

  /** 뒤로 키: 웹 화면이 처리하면(이전 화면으로) 그대로, 첫 화면이면 앱 종료 */
  @Override public void onBackPressed() {
    if (wv == null) { super.onBackPressed(); return; }
    wv.evaluateJavascript("(window.appBack&&window.appBack())?'1':'0'", new ValueCallback<String>() {
      @Override public void onReceiveValue(String v) {
        if ("\"1\"".equals(v)) return;
        if (wv.canGoBack()) wv.goBack(); else finish();
      }
    });
  }

  @Override protected void onActivityResult(int req, int res, Intent data) { super.onActivityResult(req, res, data); if (chrome != null) chrome.onResult(req, res, data); }

  @Override protected void onPause() { super.onPause(); if (wv != null) wv.onPause(); }
  @Override protected void onResume() { super.onResume(); if (wv != null) wv.onResume(); GolfWidget.refreshAll(this); }

  static String mime(String p) {
    String l = p.toLowerCase();
    if (l.endsWith(".html") || l.endsWith(".htm")) return "text/html";
    if (l.endsWith(".js")) return "application/javascript";
    if (l.endsWith(".css")) return "text/css";
    if (l.endsWith(".json") || l.endsWith(".webmanifest")) return "application/json";
    if (l.endsWith(".png")) return "image/png";
    if (l.endsWith(".webp")) return "image/webp";
    if (l.endsWith(".jpg") || l.endsWith(".jpeg")) return "image/jpeg";
    if (l.endsWith(".svg")) return "image/svg+xml";
    if (l.endsWith(".mp3")) return "audio/mpeg";
    if (l.endsWith(".woff2")) return "font/woff2";
    if (l.endsWith(".woff")) return "font/woff";
    return "application/octet-stream";
  }
}
