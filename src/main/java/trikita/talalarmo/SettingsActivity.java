package trikita.talalarmo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import trikita.jedux.Action;

public class SettingsActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (App.getState().settings().theme() == 0) {
                setTheme(android.R.style.Theme_Holo_Light);
            } else {
                setTheme(android.R.style.Theme_Holo);
            }
        } else {
            if (App.getState().settings().theme() == 0) {
                setTheme(android.R.style.Theme_Material_Light);
            } else {
                setTheme(android.R.style.Theme_Material);
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        SettingsFragment preferenceFragment = buildSettingsFragment();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();
    }

    private SettingsFragment buildSettingsFragment() {
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Boolean disableShakeSetting = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null;
        SettingsFragment preferenceFragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(SettingsFragment.DISABLE_SHAKE_SETTING, disableShakeSetting);
        preferenceFragment.setArguments(args);
        return preferenceFragment;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "vibration_setting":
                App.dispatch(new Action<>(Actions.Settings.SET_VIBRATE, prefs.getBoolean(key, false)));
                break;
            case "ramping_setting":
                App.dispatch(new Action<>(Actions.Settings.SET_RAMPING, prefs.getBoolean(key, true)));
                break;
            case "snap_setting":
                App.dispatch(new Action<>(Actions.Settings.SET_SNAP, prefs.getBoolean(key, true)));
                break;
            case "ringtone_setting":
                String s = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
                if (prefs.getString(key, s).startsWith("content://media/external/audio/media/") &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                }
                App.dispatch(new Action<>(Actions.Settings.SET_RINGTONE, prefs.getString(key, s)));
                break;
            case "theme_setting":
                int themeIndex = 0;
                try {
                    themeIndex = Integer.valueOf(prefs.getString("theme_setting", "0"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                App.dispatch(new Action<>(Actions.Settings.SET_THEME, themeIndex));
                break;
            case "clock_format_setting":
                App.dispatch(new Action<>(Actions.Settings.SET_DETECT_CLOCK_FORMAT, prefs.getBoolean(key, false)));
                break;
            case "shake_setting":
                App.dispatch(new Action<>(Actions.Settings.SET_SHAKE, prefs.getBoolean(key, false)));
                break;
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        public static final String DISABLE_SHAKE_SETTING = "DISABLE_SHAKE_SETTING";

        private Boolean disableShakeSetting = false;

        @Override
        public void setArguments(Bundle args) {
            super.setArguments(args);
            disableShakeSetting = args.getBoolean(DISABLE_SHAKE_SETTING, false);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            if(disableShakeSetting) {
                Preference preference = findPreference("shake_setting");
                preference.setShouldDisableView(true);
                preference.setEnabled(false);
            }
        }
    }
}
