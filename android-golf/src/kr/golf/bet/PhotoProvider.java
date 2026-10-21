package kr.golf.bet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** 카메라 앱이 찍은 사진을 앱 캐시에 바로 쓰게 해주는 최소 제공자 (content://kr.golf.bet.photos/파일명) */
public class PhotoProvider extends ContentProvider {
  @Override public boolean onCreate() { return true; }
  File fileFor(Uri u) {
    String n = u.getLastPathSegment(); if (n == null) n = "x.jpg";
    n = n.replaceAll("[^A-Za-z0-9._-]", "");
    File d = new File(getContext().getCacheDir(), "cam"); d.mkdirs(); return new File(d, n);
  }
  @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.parseMode(mode == null ? "r" : mode));
  }
  @Override public Cursor query(Uri uri, String[] proj, String sel, String[] args, String sort) {
    File f = fileFor(uri);
    MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
    c.addRow(new Object[]{f.getName(), f.length()});
    return c;
  }
  @Override public String getType(Uri uri) { return "image/jpeg"; }
  @Override public Uri insert(Uri uri, ContentValues v) { return null; }
  @Override public int delete(Uri uri, String s, String[] a) { return 0; }
  @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
