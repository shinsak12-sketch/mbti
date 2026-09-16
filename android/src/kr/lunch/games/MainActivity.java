package kr.lunch.games;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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

/** 점심 복불복: 로컬 assets/www 를 https://app.local 로 서빙하는 WebView 껍데기. */
public class MainActivity extends Activity {
  private WebView wv;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setStatusBarColor(Color.parseColor("#0d3628"));
    getWindow().setNavigationBarColor(Color.parseColor("#082a1f"));

    wv = new WebView(this);
    WebSettings s = wv.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    wv.setBackgroundColor(Color.parseColor("#0d3628"));
    wv.setOverScrollMode(View.OVER_SCROLL_NEVER);

    wv.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
        Uri u = req.getUrl();
        if (u == null || !"app.local".equals(u.getHost())) return null;
        String path = u.getPath();
        if (path == null || path.equals("/") || path.isEmpty()) path = "/index.html";
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (!last.contains(".")) path = path + ".html";
        try {
          InputStream in = getAssets().open("www" + path);
          WebResourceResponse r = new WebResourceResponse(mime(path), "utf-8", in);
          HashMap<String, String> h = new HashMap<>();
          h.put("Access-Control-Allow-Origin", "*");
          h.put("Cache-Control", "no-cache");
          r.setResponseHeaders(h);
          return r;
        } catch (IOException e) {
          return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
              new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
        }
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
        Uri u = req.getUrl();
        if (u != null && "app.local".equals(u.getHost())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
        return true;
      }
    });
    wv.setWebChromeClient(new WebChromeClient());
    setContentView(wv);
    wv.loadUrl("https://app.local/index.html");
  }

  @Override public void onBackPressed() {
    if (wv != null && wv.canGoBack()) wv.goBack(); else super.onBackPressed();
  }

  @Override protected void onPause() { super.onPause(); if (wv != null) wv.onPause(); }
  @Override protected void onResume() { super.onResume(); if (wv != null) wv.onResume(); }

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
