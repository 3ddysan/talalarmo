package trikita.talalarmo.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.talalarmo.Actions;
import trikita.talalarmo.MainActivity;
import trikita.talalarmo.State;

public class AlarmController implements Store.Middleware<Action, State> {

    private final Context mContext;
    private final Handler handler;
    private Runnable delayedAlarmRestart;

    public AlarmController(Context c) {
        mContext = c;
        handler = new Handler();
    }

    @Override
    public void dispatch(Store<Action, State> store, Action action, Store.NextDispatcher<Action> next) {
        if (action.type == Actions.Alarm.ON) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, 1);
            store.dispatch(new Action<>(Actions.Alarm.SET_HOUR, c.get(Calendar.HOUR)));
            store.dispatch(new Action<>(Actions.Alarm.SET_MINUTE, c.get(Calendar.MINUTE)));
            store.dispatch(new Action<>(Actions.Alarm.SET_AM_PM, c.get(Calendar.AM_PM) == 0));
        }
        next.dispatch(action);
        if (action.type instanceof Actions.Alarm) {
            Actions.Alarm type = (Actions.Alarm) action.type;
            State state = store.getState();
            switch (type) {
                case SET_HOUR:
                case SET_MINUTE:
                case SET_AM_PM:
                case TOGGLE_REPEAT_ON_DAY:
                case ADVANCED_REPEAT_ON_DAY:
                case RESTART_ALARM:
                    restartAlarmDelayed(state);
                    break;
                case WAKEUP:
                    wakeupAlarm();
                    break;
                case DISMISS:
                    dismissAlarm();
                    restartAlarm(state);
                    break;
                case OFF:
                    cancelAlarm();
                    break;
            }
        }
    }

    private void restartAlarmDelayed(final State state) {
        if(delayedAlarmRestart != null)
            handler.removeCallbacks(delayedAlarmRestart);

        handler.postDelayed(delayedAlarmRestart = () -> {
            delayedAlarmRestart = null;
            restartAlarm(state);
        }, 1000);
    }

    private void restartAlarm(State state) {
        final Calendar c = state.alarm().nextAlarm();
        Log.d("AlarmService", "Alarm on " + c.getTime());
        Intent intent = new Intent(mContext, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);

        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {        // KITKAT and later
                am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
            }
            intent = new Intent("android.intent.action.ALARM_CHANGED");
            intent.putExtra("alarmSet", true);
            mContext.sendBroadcast(intent);
            SimpleDateFormat fmt = new SimpleDateFormat("E HH:mm");
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.NEXT_ALARM_FORMATTED,
                    fmt.format(c.getTime()));
        } else {
            Intent showIntent = new Intent(mContext, MainActivity.class);
            PendingIntent showOperation = PendingIntent.getActivity(mContext, 0, showIntent, 0);
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(c.getTimeInMillis(), showOperation);
            am.setAlarmClock(alarmClockInfo, sender);
        }
    }

    private void cancelAlarm() {
        Intent intent = new Intent(mContext, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent("android.intent.action.ALARM_CHANGED");
            intent.putExtra("alarmSet", false);
            mContext.sendBroadcast(intent);
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.NEXT_ALARM_FORMATTED, "");
        }
    }

    private void wakeupAlarm() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl =
                pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "AlarmReceiver");
        wl.acquire(5000);
        mContext.startService(new Intent(mContext, AlarmService.class));
    }

    private void dismissAlarm() {
        mContext.stopService(new Intent(mContext, AlarmService.class));
    }
}
