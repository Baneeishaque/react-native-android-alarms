package com.dawnchorus.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;
import android.media.MediaPlayer;
import android.media.RingtoneManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.ReactActivity;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class LauncherModule extends ReactContextBaseJavaModule {

  private MediaPlayer mediaPlayer;
  private Uri uri;
  private ReactApplicationContext reactContext;

  public LauncherModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    uri = initRingtone();
  }

  private Uri initRingtone() {
    int[] trySounds = new int[] { RingtoneManager.TYPE_ALARM };
    int i = 0;
    while (i < trySounds.length) {
      Uri uri = RingtoneManager.getDefaultUri(trySounds[i]);
      i++;
      if (uri != null)
        return uri;
    }
    return null;
  }

  @Override
  public String getName() {
    return "AndroidAlarms";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    return constants;
  }

  // Runs android alarm ringtone
  @ReactMethod
  public final void startAlarm() {
    mediaPlayer = new MediaPlayer();
    try {
      mediaPlayer.setLooping(true);
      mediaPlayer.setDataSource(getReactApplicationContext(), uri);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch (Exception ex) {
      Log.i("ALARM MESSAGE", (ex == null ? "Error Message was null" : ex.getMessage()));
      ex.printStackTrace();
    }
  }

  @ReactMethod
  public final void stopAlarm() {
    if (mediaPlayer == null)
      return;
    mediaPlayer.stop();
  }

  // Simulates Home button and minimizes the app
  @ReactMethod
  public final void minimizeApp() {
    Intent startMain = new Intent(Intent.ACTION_MAIN);
    startMain.addCategory(Intent.CATEGORY_HOME);
    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    reactContext.startActivity(startMain);
  }

  /**
   * Creates or overwrites an alarm that launches the main application at the
   * specified timestamp. You can set multiple alarms by using different ids.
   *
   * @param id        The id identifying this alarm.
   * @param timestamp When to fire off the alarm.
   * @param inexact   Determines if the alarm should be inexact to save on battery
   *                  power.
   * @param repeats Wether the alarm repeats the same day every week or not
   */
  @ReactMethod
  public final void setAlarm(String id, double timestamp, boolean inexact, boolean repeats) {
    PendingIntent pendingIntent = createPendingIntent(id);
    long timestampLong = (long) timestamp; // React Bridge doesn't understand longs
    // get the alarm manager, and schedule an alarm that calls the receiver
    // We will use setAlarmClock because we want an indicator to show in the status
    // bar.
    // If you want to modify it and are unsure what to method to use, check
    // https://plus.google.com/+AndroidDevelopers/posts/GdNrQciPwqo

    // Put the alarm into the preferences
    SharedPreferences alarms = getReactApplicationContext().getSharedPreferences("Alarms", 0);
    SharedPreferences.Editor editor = alarms.edit();
    editor.putLong(id, timestampLong);
    // Commit the edits!
    editor.commit();

    if (repeats) {
        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, timestampLong,AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    } else {
        if (!inexact) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getAlarmManager().setAlarmClock(new AlarmManager.AlarmClockInfo(timestampLong, pendingIntent), pendingIntent);
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, timestampLong, pendingIntent);
            else
                getAlarmManager().set(AlarmManager.RTC_WAKEUP, timestampLong, pendingIntent);
        } else {
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, timestampLong, pendingIntent);
        }
    }

    Context context = getReactApplicationContext();
  }

  @ReactMethod
  public final void clearAlarm(String id) {
    // Clear alarm from the preferences
    SharedPreferences alarms = getReactApplicationContext().getSharedPreferences("Alarms", 0);
    SharedPreferences.Editor editor = alarms.edit();
    editor.remove(id);
    editor.commit();

    PendingIntent pendingIntent = createPendingIntent(id);
    getAlarmManager().cancel(pendingIntent);
  }

  private PendingIntent createPendingIntent(String id) {
    Context context = getReactApplicationContext();
    // create the pending intent
    Intent intent = new Intent(context, AlarmReceiver.class);
    // set unique alarm ID to identify it. Used for clearing and seeing which one
    // fired
    // public boolean filterEquals(Intent other) compare the action, data, type,
    // package, component, and categories, but do not compare the extra
    intent.setData(Uri.parse("id://" + id));
    intent.setAction(String.valueOf(id));
    return PendingIntent.getBroadcast(context, 0, intent, 0);
  }

  private AlarmManager getAlarmManager() {
    return (AlarmManager) getReactApplicationContext().getSystemService(Context.ALARM_SERVICE);
  }
}
