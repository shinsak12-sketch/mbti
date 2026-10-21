package kr.golf.bet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

/** 위젯에서 뜨는 홈 화면 위 팝업 (기록 · 예정 입력, 기록 메뉴). 저장하면 닫히고 위젯이 바로 갱신됨. */
public class QuickActivity extends Activity {
  private WebView wv;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    wv = new WebView(this);
    WebSettings s = wv.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);
    s.setSupportZoom(false);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    wv.setBackgroundColor(Color.TRANSPARENT);
    wv.setOverScrollMode(View.OVER_SCROLL_NEVER);
    wv.setWebViewClient(new AppWebClient(this, null));
    wv.setWebChromeClient(new WebChromeClient());
    wv.addJavascriptInterface(new GolfBridge(this), "GolfNative");
    setContentView(wv);
    load(getIntent());
  }

  @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); load(i); }

  private void load(Intent i) {
    String a = "add", d = "", id = "";
    if (i != null) {
      if (i.getStringExtra("a") != null) a = i.getStringExtra("a");
      if (i.getStringExtra("d") != null) d = i.getStringExtra("d");
      if (i.getStringExtra("id") != null) id = i.getStringExtra("id");
      Uri u = i.getData();
      if (u != null) {
        if (u.getQueryParameter("a") != null) a = u.getQueryParameter("a");
        if (u.getQueryParameter("d") != null) d = u.getQueryParameter("d");
        if (u.getQueryParameter("id") != null) id = u.getQueryParameter("id");
      }
    }
    wv.loadUrl("https://app.local/index.html?popup=1&a=" + MainActivity.safe(a) + "&d=" + MainActivity.safe(d) + "&id=" + MainActivity.safe(id));
  }

  @Override public void onBackPressed() {
    wv.evaluateJavascript("(window.appBack&&window.appBack())?'1':'0'", new ValueCallback<String>() {
      @Override public void onReceiveValue(String v) { if (!"\"1\"".equals(v)) finish(); }
    });
  }
  @Override protected void onPause() { super.onPause(); GolfWidget.refreshAll(this); }
}
