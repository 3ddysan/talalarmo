package trikita.talalarmo.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.Map;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.talalarmo.Actions;
import trikita.talalarmo.App;
import trikita.talalarmo.MainActivity;
import trikita.talalarmo.R;

import static trikita.anvil.BaseDSL.MATCH;
import static trikita.anvil.DSL.CENTER;
import static trikita.anvil.DSL.CENTER_VERTICAL;
import static trikita.anvil.DSL.FILL;
import static trikita.anvil.DSL.LEFT;
import static trikita.anvil.DSL.WRAP;
import static trikita.anvil.DSL.allCaps;
import static trikita.anvil.DSL.backgroundColor;
import static trikita.anvil.DSL.backgroundDrawable;
import static trikita.anvil.DSL.button;
import static trikita.anvil.DSL.checkBox;
import static trikita.anvil.DSL.checked;
import static trikita.anvil.DSL.dip;
import static trikita.anvil.DSL.enabled;
import static trikita.anvil.DSL.frameLayout;
import static trikita.anvil.DSL.gravity;
import static trikita.anvil.DSL.isPortrait;
import static trikita.anvil.DSL.layoutGravity;
import static trikita.anvil.DSL.linearLayout;
import static trikita.anvil.DSL.margin;
import static trikita.anvil.DSL.max;
import static trikita.anvil.DSL.onCheckedChange;
import static trikita.anvil.DSL.onClick;
import static trikita.anvil.DSL.onSeekBarChange;
import static trikita.anvil.DSL.orientation;
import static trikita.anvil.DSL.padding;
import static trikita.anvil.DSL.pressed;
import static trikita.anvil.DSL.progress;
import static trikita.anvil.DSL.size;
import static trikita.anvil.DSL.text;
import static trikita.anvil.DSL.textColor;
import static trikita.anvil.DSL.textSize;
import static trikita.anvil.DSL.textView;
import static trikita.anvil.DSL.typeface;
import static trikita.anvil.DSL.v;
import static trikita.anvil.DSL.visibility;
import static trikita.anvil.DSL.weight;
import static trikita.anvil.DSL.x;
import static trikita.anvil.DSL.y;

public class AlarmLayout {
    public static void view() {
        backgroundColor(Theme.get(App.getState().settings().theme()).backgroundColor);
        boolean on = App.getState().alarm().on();
        linearLayout(() -> {
            orientation(LinearLayout.VERTICAL);
            header();
            frameLayout(() -> {
                size(FILL, 0);
                weight(1f);
                if (on) {
                    alarmOnLayout();
                } else {
                    alarmOffLayout();
                }
            });
            if (on)
                advancedRepeatLayout(Anvil.currentView().getContext());
            bottomBar();
        });
    }

    private static void advancedRepeatLayout(Context context) {
        Theme theme = Theme.get(App.getState().settings().theme());
        boolean isAdvancedRepeatEnabled = App.getState().alarm().advanced();

        linearLayout(() -> {
            size(MATCH, dip(isPortrait() ? 75 : 40));
            backgroundColor(theme.backgroundColor);
            orientation(isPortrait() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            gravity(CENTER);

            advancedRepeatCheckbox(isAdvancedRepeatEnabled, v -> {
                App.dispatch(new Action<>(Actions.Alarm.ADVANCED_REPEAT_ON_DAY, (!isAdvancedRepeatEnabled)));
            });


            linearLayout(() -> {
                orientation(LinearLayout.HORIZONTAL);
                gravity(CENTER);
                size(MATCH, dip(40));

                if (isAdvancedRepeatEnabled) {
                    String[] dayLabels = context.getResources().getStringArray(R.array.repeat_days);
                    final Map<Integer, Boolean> repeatOnDays = App.getState().alarm().repeatOnDays();
                    int labelIndex = 0;
                    for (Map.Entry<Integer, Boolean> repeatEntry : repeatOnDays.entrySet()) {
                        final String label = dayLabels[labelIndex++];
                        final Integer day = repeatEntry.getKey();
                        button(() ->{
                            Drawable bg = Anvil.currentView().getResources().getDrawable(R.drawable.oval_shape);
                            Boolean isPressed = repeatOnDays.get(day);
                            if (isPressed) {
                                bg.setColorFilter(theme.accentColor, PorterDuff.Mode.SRC_ATOP);
                            } else {
                                bg.setColorFilter(theme.primaryDarkColor, PorterDuff.Mode.SRC_ATOP);
                            }
                            backgroundDrawable(bg);
                            size(dip(40), dip(40));
                            text(label);
                            textColor(theme.primaryTextColor);
                            pressed(isPressed);
                            enabled(true);
                            onClick(v -> {
                                if(App.getState().alarm().repeatOnDaysCount() == 1 && isPressed) {
                                    App.dispatch(new Action<>(Actions.Alarm.ADVANCED_REPEAT_ON_DAY, false));
                                } else {
                                    App.dispatch(new Action<>(Actions.Alarm.TOGGLE_REPEAT_ON_DAY, day));
                                }
                            });
                        });
                    }
                }

            });

        });
    }

    private static void advancedRepeatCheckbox(boolean isRepeatOnDaysActive, View.OnClickListener onClickListener) {
        final Theme theme = Theme.get(App.getState().settings().theme());
        linearLayout(() -> {
            backgroundColor(theme.backgroundColor);
            orientation(LinearLayout.HORIZONTAL);
            gravity(CENTER);

            checkBox(() -> {
                onClick(onClickListener);
                checked(isRepeatOnDaysActive);
            });
            textView(() -> {
                text(R.string.settings_advanced_repeat);
                textColor(theme.primaryTextColor);
                onClick(onClickListener);
            });
        });
    }

    private static void header() {
        linearLayout(() -> {
            size(FILL, WRAP);
            gravity(CENTER_VERTICAL);
            Theme.materialIcon(() -> {
                textColor(Theme.get(App.getState().settings().theme()).secondaryTextColor);
                textSize(dip(32));
                padding(dip(15));
                text("\ue855"); // "alarm" icon
                onClick(v -> App.dispatch(new Action<>(Actions.Alarm.WAKEUP)));
            });
            textView(() -> {
                size(WRAP, WRAP);
                weight(1f);
                typeface("fonts/Roboto-Light.ttf");
                textSize(dip(20));
                textColor(Theme.get(App.getState().settings().theme()).primaryTextColor);
                text(R.string.app_name);
            });
        });
    }

    private static void alarmOffLayout() {
        textView(() -> {
            size(FILL, FILL);
            padding(dip(20));
            gravity(LEFT | CENTER_VERTICAL);
            typeface("fonts/Roboto-Light.ttf");
            allCaps(true);
            textSize(dip(32));
            textColor(Theme.get(App.getState().settings().theme()).primaryTextColor);
            text(R.string.tv_start_alarm_text);
            onClick(v -> App.dispatch(new Action<>(Actions.Alarm.ON)));
        });
    }

    private static void alarmOnLayout() {
        frameLayout(() -> {
            size(FILL, FILL);
            // On tablets leave some margin around the clock view to avoid gigantic circles
            if ((Anvil.currentView().getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
                margin(dip(48));
            } else {
                margin(dip(8));
            }
            int w = Anvil.currentView().getWidth();
            int h = Anvil.currentView().getHeight();
            if (h == 0 || w == 0) {
                Anvil.currentView().post(Anvil::render);
            }

            int hourCircleSize;
            int minuteCircleSize;
            int amPmWidth;

            if (isPortrait()) {
                hourCircleSize = (int) (w * 1.1f * 0.62f);
                minuteCircleSize = (int) (hourCircleSize * 0.62f);
                amPmWidth = (int) (hourCircleSize * 0.62f * 0.62f);
            } else {
                hourCircleSize = (int) (h);
                minuteCircleSize = (int) (hourCircleSize * 0.62f);
                amPmWidth = (int) (hourCircleSize * 0.62f * 0.62f);
            }

            final boolean is24hFormat = App.getState().settings().detectClockFormat() &&
                    DateFormat.is24HourFormat(Anvil.currentView().getContext());

            frameLayout(() -> {
                size(hourCircleSize, hourCircleSize);
                if (isPortrait()) {
                    x(w / 2 - hourCircleSize * 0.21f - hourCircleSize / 2);
                    y(h / 2 + hourCircleSize * 0.19f - hourCircleSize / 2);
                } else {
                    x(w / 2 - hourCircleSize * 0.38f - hourCircleSize / 2);
                    y(h / 2 + hourCircleSize * 0.00f - hourCircleSize / 2);
                }
                gravity(CENTER);

                final boolean isPM = !App.getState().alarm().am();
                int hours = App.getState().alarm().hours() + (is24hFormat && isPM ? 12 : 0);

                v(ClockView.class, () -> {
                    size(FILL, FILL);
                    progress(hours);
                    max(is24hFormat ? 24 : 12);
                    onSeekBarChange((v, progress, fromUser) -> {
                        if (fromUser) {
                            if(is24hFormat) {
                                App.dispatch(new Action<>(Actions.Alarm.SET_AM_PM, progress < 12));
                            }
                            App.dispatch(new Action<>(Actions.Alarm.SET_HOUR, progress % 12));
                        }
                    });
                    Anvil.currentView().invalidate();
                });
                textView(() -> {
                    size(WRAP, WRAP);
                    if (is24hFormat) {
                        text(String.format("%02d", hours));
                    } else {
                        if (hours == 0) {
                            text("12");
                        } else {
                            text(String.format("%02d", hours));
                        }
                    }
                    layoutGravity(CENTER);
                    typeface("fonts/Roboto-Light.ttf");
                    textSize(hourCircleSize * 0.3f);
                    textColor(Theme.get(App.getState().settings().theme()).primaryColor);
                });
            });

            frameLayout(() -> {
                size(minuteCircleSize, minuteCircleSize);
                if (isPortrait()) {
                    x(w / 2 - hourCircleSize * 0.25f + minuteCircleSize / 2);
                    y(h / 2 + hourCircleSize * 0.05f - hourCircleSize / 2 - minuteCircleSize / 2);
                } else {
                    x(w / 2 - hourCircleSize * 0.25f + minuteCircleSize / 2);
                    y(h / 2 + hourCircleSize * 0.28f - hourCircleSize / 2 - minuteCircleSize / 2);
                }
                gravity(CENTER);
                v(ClockView.class, () -> {
                    size(FILL, FILL);
                    progress(App.getState().alarm().minutes());
                    max(60);
                    onSeekBarChange((v, progress, fromUser) -> {
                        if (fromUser) {
                            if (App.getState().settings().snap()) {
                                progress = (int) (Math.round(progress / 5.0) * 5) % 60;
                            }
                            App.dispatch(new Action<>(Actions.Alarm.SET_MINUTE, progress));
                        }
                    });
                    Anvil.currentView().invalidate();
                });
                textView(() -> {
                    size(WRAP, WRAP);
                    text(String.format("%02d", App.getState().alarm().minutes()));
                    layoutGravity(CENTER);
                    typeface("fonts/Roboto-Light.ttf");
                    textSize(minuteCircleSize * 0.3f);
                    textColor(Theme.get(App.getState().settings().theme()).primaryColor);
                });
            });

            if(!is24hFormat) {
                v(AmPmSwitch.class, () -> {
                    size(amPmWidth, (int) (amPmWidth / 1.5f));
                    if (isPortrait()) {
                        x(w / 2 - hourCircleSize * 0.21f - amPmWidth * 3 / 4);
                        y(h / 2 + hourCircleSize * 0.05f - hourCircleSize / 2 - amPmWidth / 1.5f / 2);
                    } else {
                        x(w / 2 - hourCircleSize * 0.25f + minuteCircleSize - amPmWidth / 2);
                        y(h / 2 + hourCircleSize * 0.25f - amPmWidth / 1.5f / 2);
                    }
                    checked(App.getState().alarm().am());
                    onCheckedChange((CompoundButton buttonView, boolean isChecked) -> {
                        App.dispatch(new Action<>(Actions.Alarm.SET_AM_PM, isChecked));
                    });
                });
            }
        });
    }

    private static void bottomBar() {
        linearLayout(() -> {
            size(FILL, dip(62));
            backgroundColor(Theme.get(App.getState().settings().theme()).backgroundTranslucentColor);

            Theme.materialIcon(() -> {
                text("\ue857"); // ALARM OFF
                textSize(dip(32));
                textColor(Theme.get(App.getState().settings().theme()).secondaryTextColor);
                padding(dip(15));
                visibility(App.getState().alarm().on());
                onClick(v -> App.dispatch(new Action<>(Actions.Alarm.OFF)));
            });

            textView(() -> {
                size(0, FILL);
                weight(1f);
                margin(dip(10), 0);
                typeface("fonts/Roboto-Light.ttf");
                textSize(dip(16));
                textColor(Theme.get(App.getState().settings().theme()).primaryTextColor);
                gravity(CENTER | CENTER_VERTICAL);
                text(formatAlarmTime(Anvil.currentView().getContext()));
            });

            Theme.materialIcon(() -> {
                text("\ue5d4"); // "more vert"
                textSize(dip(32));
                textColor(Theme.get(App.getState().settings().theme()).secondaryTextColor);
                padding(dip(15));
                onClick(AlarmLayout::showSettingsMenu);
            });
        });
    }

    private static String formatAlarmTime(Context c) {
        if (!App.getState().alarm().on()) {
            return "";
        }
        long t = App.getState().alarm().nextAlarm().getTimeInMillis() - System.currentTimeMillis() - 1;
        t = t / 60 / 1000;
        int m = (int) (t % 60);
        int h = (int) (t / 60);

        String minSeq = (m == 0) ? "" :
                (m == 1) ? c.getString(R.string.minute) :
                        c.getString(R.string.minutes, Long.toString(m));

        String hourSeq = (h == 0) ? "" :
                (h == 1) ? c.getString(R.string.hour) :
                        c.getString(R.string.hours, Long.toString(h));

        int index = ((h > 0) ? 1 : 0) | ((m > 0) ? 2 : 0);

        String[] formats = c.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], hourSeq, minSeq);
    }

    private static void showSettingsMenu(View v) {
        ((MainActivity) v.getContext()).openSettings();
    }
}
