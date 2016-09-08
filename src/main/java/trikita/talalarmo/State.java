package trikita.talalarmo;

import android.media.RingtoneManager;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import trikita.jedux.Action;
import trikita.jedux.Store;

@Value.Immutable
@Gson.TypeAdapters
public interface State {

    @Value.Immutable
    interface Settings {
        boolean vibrate();

        boolean snap();

        boolean ramping();

        String ringtone();

        int theme();

        boolean detectClockFormat();

        boolean shake();

    }

    @Value.Immutable
    abstract class Alarm {
        public abstract boolean on();

        public abstract int minutes();

        public abstract int hours();

        public abstract boolean am();

        public abstract Map<Integer, Boolean> repeatOnDays();

        public int repeatOnDaysCount() {
            int counter = 0;
            for(Boolean isActive : repeatOnDays().values()) {
                if(isActive)
                    counter++;
            }
            return counter;
        }

        public abstract Boolean advanced();

        public Calendar nextAlarm() {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.AM_PM, (am() ? Calendar.AM : Calendar.PM));
            c.set(Calendar.HOUR, hours());
            c.set(Calendar.MINUTE, minutes());
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            if (System.currentTimeMillis() >= c.getTimeInMillis()) {
                c.add(Calendar.DATE, 1);
            }

            if(advanced()) {
                return nextAlarmAdvanced(c);
            }

            return c;
        }

        private Calendar nextAlarmAdvanced(Calendar currentNextAlarm) {

            if(isAfterNowAndRepeatOn(currentNextAlarm)){
                return currentNextAlarm;
            }

            int firstDayOfWeekRepeatIsEnabled = -1;

            for(Map.Entry<Integer, Boolean> entry : repeatOnDays().entrySet()) {
                final Integer dayID = entry.getKey();
                final Boolean repeatIsEnabled = entry.getValue();

                if(!repeatIsEnabled)
                    continue;

                final Calendar potentialNextAlarm = buildPotentialNextAlarm(currentNextAlarm, dayID);

                if(firstDayOfWeekRepeatIsEnabled == -1) {
                    firstDayOfWeekRepeatIsEnabled = dayID;
                }

                if (isAfterOrSameDay(currentNextAlarm, potentialNextAlarm)) {
                    return potentialNextAlarm;
                }
            }
            if(firstDayOfWeekRepeatIsEnabled != -1) {// schedule for next week, the very first day, which is enabled
                currentNextAlarm.set(Calendar.DAY_OF_WEEK, firstDayOfWeekRepeatIsEnabled);
                currentNextAlarm.add(Calendar.WEEK_OF_MONTH, 1);
                return currentNextAlarm;
            }
            throw new IllegalStateException("Next Alarm may only be in range Monday to Sunday.");
        }

        private Calendar buildPotentialNextAlarm(Calendar currentNextAlarm, Integer dayID) {
            final Calendar potentialNextAlarm = (Calendar) currentNextAlarm.clone();
            potentialNextAlarm.set(Calendar.DAY_OF_WEEK, dayID);
            if(dayID == Calendar.SUNDAY) {
                potentialNextAlarm.add(Calendar.DAY_OF_WEEK, 7); // monday is first day of week
            }
            return potentialNextAlarm;
        }

        private boolean isAfterNowAndRepeatOn(Calendar currentNextAlarm) {
            return currentNextAlarm.after(Calendar.getInstance()) && repeatOnDays().get(currentNextAlarm.get(Calendar.DAY_OF_WEEK));
        }

        private boolean isAfterOrSameDay(Calendar currentNextAlarm, Calendar potentialNextAlarm) {
            return isSameDay(potentialNextAlarm, currentNextAlarm) || potentialNextAlarm.after(currentNextAlarm);
        }

        private Boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

    }

    Settings settings();

    Alarm alarm();

    class Default {
        static final Map<Integer, Boolean> REPEAT_ON_ALL_DAYS;
        static {
            Map<Integer, Boolean> map = new LinkedHashMap<>();
            map.put(Calendar.MONDAY, true);
            map.put(Calendar.TUESDAY, true);
            map.put(Calendar.WEDNESDAY, true);
            map.put(Calendar.THURSDAY, true);
            map.put(Calendar.FRIDAY, true);
            map.put(Calendar.SATURDAY, true);
            map.put(Calendar.SUNDAY, true);
            REPEAT_ON_ALL_DAYS = Collections.unmodifiableMap(map);
        }
        public static State build() {
            return ImmutableState.builder()
                    .alarm(ImmutableAlarm.builder()
                            .on(false)
                            .am(false)
                            .hours(10)
                            .minutes(0)
                            .repeatOnDays(REPEAT_ON_ALL_DAYS)
                            .advanced(false)
                            .build())
                    .settings(ImmutableSettings.builder()
                            .ringtone(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
                            .ramping(true)
                            .snap(true)
                            .vibrate(true)
                            .theme(0)
                            .detectClockFormat(false)
                            .shake(false)
                            .build())
                    .build();
        }
    }

    class Reducer implements Store.Reducer<Action, State> {
        public State reduce(Action action, State currentState) {
            return ImmutableState.builder().from(currentState)
                    .alarm(reduceAlarm(action, currentState.alarm()))
                    .settings(reduceSettings(action, currentState.settings()))
                    .build();
        }

        State.Settings reduceSettings(Action action, State.Settings settings) {
            if (action.type instanceof Actions.Settings) {
                Actions.Settings type = (Actions.Settings) action.type;
                switch (type) {
                    case SET_RAMPING:
                        return ImmutableSettings.copyOf(settings).withRamping((Boolean) action.value);
                    case SET_VIBRATE:
                        return ImmutableSettings.copyOf(settings).withVibrate((Boolean) action.value);
                    case SET_SNAP:
                        return ImmutableSettings.copyOf(settings).withSnap((Boolean) action.value);
                    case SET_RINGTONE:
                        return ImmutableSettings.copyOf(settings).withRingtone((String) action.value);
                    case SET_THEME:
                        return ImmutableSettings.copyOf(settings).withTheme((Integer) action.value);
                    case SET_DETECT_CLOCK_FORMAT:
                        return ImmutableSettings.copyOf(settings).withDetectClockFormat((Boolean) action.value);
                    case SET_SHAKE:
                        return ImmutableSettings.copyOf(settings).withShake((Boolean) action.value);
                }
            }
            return settings;
        }

        State.Alarm reduceAlarm(Action action, State.Alarm alarm) {
            if (action.type instanceof Actions.Alarm) {
                Actions.Alarm type = (Actions.Alarm) action.type;
                switch (type) {
                    case ON:
                        return ImmutableAlarm.copyOf(alarm).withOn(true);
                    case OFF:
                        return ImmutableAlarm.copyOf(alarm).withOn(false).withAdvanced(false).withRepeatOnDays(Default.REPEAT_ON_ALL_DAYS);
                    case SET_MINUTE:
                        return ImmutableAlarm.copyOf(alarm).withMinutes((Integer) action.value);
                    case SET_HOUR:
                        return ImmutableAlarm.copyOf(alarm).withHours((Integer) action.value);
                    case SET_AM_PM:
                        return ImmutableAlarm.copyOf(alarm).withAm((Boolean) action.value);
                    case TOGGLE_REPEAT_ON_DAY:
                        final Integer day = (Integer) action.value;
                        final Map<Integer, Boolean> repeatOnDays = new LinkedHashMap<>(alarm.repeatOnDays());
                        repeatOnDays.put(day, !repeatOnDays.get(day));
                        return ImmutableAlarm.copyOf(alarm).withRepeatOnDays(repeatOnDays);
                    case ADVANCED_REPEAT_ON_DAY:
                        final Boolean repeat = (Boolean) action.value;
                        return ImmutableAlarm.copyOf(alarm).withAdvanced(repeat).withRepeatOnDays(Default.REPEAT_ON_ALL_DAYS);
                }
            }
            return alarm;
        }
    }
}
