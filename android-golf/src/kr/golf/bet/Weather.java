package kr.golf.bet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/** 라운드 알림에 붙일 날씨 한 줄 (Open-Meteo · 키 없이 무료 · 해외도 됨) */
public class Weather {
  static String sky(int c) {
    if (c == 0) return "맑음"; if (c <= 2) return "구름 조금"; if (c == 3) return "흐림"; if (c == 45 || c == 48) return "안개";
    if (c >= 51 && c <= 57) return "이슬비"; if (c >= 61 && c <= 67) return "비"; if (c >= 71 && c <= 77) return "눈";
    if (c >= 80 && c <= 82) return "소나기"; if (c == 85 || c == 86) return "눈"; if (c >= 95) return "뇌우"; return "";
  }

  /** [한 줄, 비 예보면 "1"] · 실패하면 null */
  static String[] line(double la, double lo, String date, String tee) {
    try {
      URL u = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + la + "&longitude=" + lo
        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max"
        + "&hourly=weather_code,temperature_2m,precipitation_probability,wind_speed_10m&wind_speed_unit=ms&timezone=auto&forecast_days=16");
      HttpURLConnection h = (HttpURLConnection) u.openConnection(); h.setConnectTimeout(8000); h.setReadTimeout(8000);
      if (h.getResponseCode() != 200) return null;
      InputStream in = h.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
      while ((n = in.read(b)) > 0) bo.write(b, 0, n); in.close();
      JSONObject j = new JSONObject(bo.toString("UTF-8")); JSONObject d = j.getJSONObject("daily"); JSONArray days = d.getJSONArray("time");
      int i = -1; for (int k = 0; k < days.length(); k++) if (date.equals(days.optString(k))) { i = k; break; }
      if (i < 0) return null;
      int code = d.getJSONArray("weather_code").optInt(i); long tmin = Math.round(d.getJSONArray("temperature_2m_min").optDouble(i)), tmax = Math.round(d.getJSONArray("temperature_2m_max").optDouble(i));
      int pop = d.getJSONArray("precipitation_probability_max").optInt(i); long wind = Math.round(d.getJSONArray("wind_speed_10m_max").optDouble(i));
      StringBuilder s = new StringBuilder(sky(code)).append(' ').append(tmin).append('~').append(tmax).append("° · 비 ").append(pop).append("% · 바람 ").append(wind).append("m/s");
      if (tee != null && tee.length() == 5) {
        JSONObject hr = j.getJSONObject("hourly"); JSONArray ts = hr.getJSONArray("time"); String key = date + "T" + tee.substring(0, 2) + ":00";
        for (int k = 0; k < ts.length(); k++) if (key.equals(ts.optString(k))) {
          s.append("\n티오프 무렵 ").append(sky(hr.getJSONArray("weather_code").optInt(k))).append(' ').append(Math.round(hr.getJSONArray("temperature_2m").optDouble(k))).append("° · 비 ").append(hr.getJSONArray("precipitation_probability").optInt(k)).append('%');
          break;
        }
      }
      return new String[]{ s.toString(), pop >= 60 || (code >= 61 && code <= 67) || (code >= 80 && code <= 82) || code >= 95 ? "1" : "0" };
    } catch (Exception e) { return null; }
  }
}
