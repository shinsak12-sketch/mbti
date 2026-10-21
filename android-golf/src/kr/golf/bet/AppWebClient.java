package kr.golf.bet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/** assets/www 를 https://app.local 로 서빙 (메인 화면 · 위젯 팝업 공용) */
public class AppWebClient extends WebViewClient {
  private final Context ctx;
  private final Runnable onFinished;
  AppWebClient(Context c, Runnable onFinished) { this.ctx = c; this.onFinished = onFinished; }

  @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
    Uri u = req.getUrl();
    if (u == null || !"app.local".equals(u.getHost())) return null;
    String path = u.getPath();
    if (path == null || path.equals("/") || path.isEmpty()) path = "/index.html";
    String last = path.substring(path.lastIndexOf('/') + 1);
    if (!last.contains(".")) path = path + ".html";
    try {
      InputStream in = ctx.getAssets().open("www" + path);
      WebResourceResponse r = new WebResourceResponse(MainActivity.mime(path), "utf-8", in);
      HashMap<String, String> h = new HashMap<>();
      h.put("Cache-Control", "no-cache");
      r.setResponseHeaders(h);
      return r;
    } catch (IOException e) {
      return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
    }
  }
  @Override public void onPageFinished(WebView v, String url) { if (onFinished != null) onFinished.run(); }
  @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
    Uri u = req.getUrl();
    if (u != null && "app.local".equals(u.getHost())) return false;
    try { Intent i = new Intent(Intent.ACTION_VIEW, u); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); } catch (Exception ignored) {}
    return true;
  }
}
