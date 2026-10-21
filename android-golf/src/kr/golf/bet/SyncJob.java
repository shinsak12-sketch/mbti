package kr.golf.bet;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

/** 15분마다 동행자가 공유한 일정 확인 → 새 일정 · 변경 · 취소 알림, 위젯 목록에도 바로 반영 */
public class SyncJob extends JobService {
  static final int JOB_ID = 4711;

  static void ensure(Context c) {
    JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (js == null) return;
    for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == JOB_ID) return;
    js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, SyncJob.class))
      .setPeriodic(15 * 60 * 1000L).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build());
  }

  @Override public boolean onStartJob(final JobParameters p) {
    new Thread(new Runnable() { @Override public void run() { try { check(getApplicationContext()); } catch (Exception ignored) {} jobFinished(p, false); } }).start();
    return true;
  }
  @Override public boolean onStopJob(JobParameters p) { return true; }


  static void check(Context c) throws Exception {
    SharedPreferences sp = c.getSharedPreferences("golf", Context.MODE_PRIVATE);
    JSONObject sum = new JSONObject(sp.getString("sum", "{}"));
    JSONObject sync = sum.optJSONObject("sync"); if (sync == null) return;
    String base = sync.optString("url"), tok = sync.optString("token"), me = sync.optString("name");
    if (base.length() == 0 || tok.length() == 0) return;
    HttpURLConnection h = (HttpURLConnection) new URL(base + "inbox").openConnection();
    h.setConnectTimeout(10000); h.setReadTimeout(10000); h.setRequestProperty("x-golf-token", tok);
    if (h.getResponseCode() != 200) return;
    InputStream in = h.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
    while ((n = in.read(b)) > 0) bo.write(b, 0, n); in.close();
    JSONObject ib = new JSONObject(bo.toString("UTF-8")); JSONObject items = ib.optJSONObject("items"); if (items == null) return;
    JSONObject seen = new JSONObject(sp.getString("nseen", "{}"));
    boolean first = !sp.contains("nseen");
    JSONArray recs = sum.optJSONArray("records"); if (recs == null) { recs = new JSONArray(); sum.put("records", recs); }
    JSONObject drop = sum.optJSONObject("dropped"); // 웹에서 지운(받기 거절한) 공유 일정
    boolean changed = false;
    for (Iterator<String> it = items.keys(); it.hasNext(); ) {
      String k = it.next(); JSONObject x = items.optJSONObject(k); if (x == null) continue;
      long u = x.optLong("u"); if (seen.optLong(k) == u) continue;
      boolean had = seen.has(k); seen.put(k, u);
      String id = "s_" + k.replace(':', '_'); int at = -1;
      for (int i = 0; i < recs.length(); i++) if (id.equals(recs.optJSONObject(i).optString("i"))) { at = i; break; }
      String from = x.optString("fromName");
      if (x.optBoolean("cancel")) {
        if (at >= 0 && recs.optJSONObject(at).optInt("p") == 1) { JSONObject r = recs.optJSONObject(at); recs.remove(at); changed = true;
          if (!first) Reminders.notifyShared(c, from + "님이 일정 취소", label(r.optString("d"), r.optString("t"), r.optString("c")), "", k.hashCode()); }
        continue;
      }
      JSONObject pl = x.optJSONObject("plan"); if (pl == null) continue;
      if (drop != null && drop.has(id)) continue;
      if (at >= 0 && recs.optJSONObject(at).optInt("p") != 1) continue; // 이미 다녀옴으로 바꾼 건 그대로
      JSONArray ms = pl.optJSONArray("mates"); StringBuilder mates = new StringBuilder(from);
      if (ms != null) for (int i = 0; i < ms.length(); i++) { String m = ms.optString(i); if (m.length() > 0 && !m.equals(me)) mates.append(", ").append(m); }
      JSONObject r = new JSONObject();
      r.put("i", id); r.put("d", pl.optString("date")); r.put("p", 1); r.put("c", pl.optString("course").length() > 0 ? pl.optString("course") : pl.optString("region"));
      r.put("g", pl.optString("region")); r.put("m", mates.toString()); r.put("k", pl.optBoolean("caddie") ? 1 : 0); r.put("h", pl.optInt("holes", 18));
      r.put("s", 0); r.put("o", u); r.put("t", pl.optString("tee"));
      if (at >= 0) recs.put(at, r); else recs.put(r);
      changed = true;
      if (!first) Reminders.notifyShared(c, from + (had ? "님이 일정 바꿈" : "님이 일정 공유"), label(r.optString("d"), r.optString("t"), r.optString("c")) + "  ·  " + mates, id, k.hashCode());
    }
    sp.edit().putString("nseen", seen.toString()).apply();
    if (changed) { sp.edit().putString("sum", sum.toString()).commit(); GolfWidget.refreshAll(c); Reminders.schedule(c); }
  }

  static String label(String d, String t, String course) {
    String s = d.length() == 10 ? Integer.parseInt(d.substring(5, 7)) + "/" + Integer.parseInt(d.substring(8, 10)) : d;
    return s + (t.length() > 0 ? " " + t : "") + "  " + course;
  }
}
