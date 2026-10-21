package kr.golf.bet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 폰을 다시 켜거나 앱이 업데이트되면 알림 다시 맞춤 */
public class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) { Reminders.schedule(c); GolfWidget.refreshAll(c); }
}
