package kr.golf.bet;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import java.io.File;

/** 웹의 사진 선택(<input type=file>) → 카메라 촬영 또는 앨범 */
public class GolfChrome extends WebChromeClient {
  static final int REQ = 4242;
  private final Activity act;
  private ValueCallback<Uri[]> cb;
  private String camName;
  GolfChrome(Activity a) { act = a; }

  @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams p) {
    if (cb != null) cb.onReceiveValue(null);
    cb = callback;
    camName = "c" + System.currentTimeMillis() + ".jpg";
    Uri camUri = Uri.parse("content://kr.golf.bet.photos/" + camName);
    Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    cam.putExtra(MediaStore.EXTRA_OUTPUT, camUri);
    cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    cam.setClipData(ClipData.newRawUri("photo", camUri));
    Intent target;
    if (p != null && p.isCaptureEnabled()) target = cam;
    else {
      Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
      pick.setType("image/*"); pick.addCategory(Intent.CATEGORY_OPENABLE); pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      target = Intent.createChooser(pick, "사진 고르기");
      target.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
    }
    try { act.startActivityForResult(target, REQ); }
    catch (Exception e) { cb.onReceiveValue(null); cb = null; return false; }
    return true;
  }

  void onResult(int req, int res, Intent data) {
    if (req != REQ || cb == null) return;
    Uri[] out = null;
    if (res == Activity.RESULT_OK) {
      if (data != null && data.getClipData() != null && data.getClipData().getItemCount() > 0 && !"photo".contentEquals(String.valueOf(data.getClipData().getDescription().getLabel()))) {
        ClipData cd = data.getClipData(); out = new Uri[cd.getItemCount()];
        for (int i = 0; i < cd.getItemCount(); i++) out[i] = cd.getItemAt(i).getUri();
      } else if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
      else {
        File f = new File(new File(act.getCacheDir(), "cam"), camName);
        if (f.exists() && f.length() > 0) out = new Uri[]{Uri.parse("content://kr.golf.bet.photos/" + camName)};
      }
    }
    cb.onReceiveValue(out); cb = null;
  }
}
