package trikita.talalarmo.alarm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeListener implements SensorEventListener{

    private final SensorManager mSensorMgr;
    private OnShake mShakeListener;

    private long mLastShakeTime;
    private static final int TIME_THREASHOLD_MILLISECS = 100;
    private static final int ACCELERATION_THREASHOLD = 30;

    public interface OnShake {
        void execute();
    }

    public ShakeListener(Context context) {
        mSensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void registerSensor(OnShake shakeListener) {
        mShakeListener = shakeListener;
        mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterSensor() {
        mSensorMgr.unregisterListener(this);
        mShakeListener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();

        if((curTime - mLastShakeTime) < TIME_THREASHOLD_MILLISECS) {
            return;
        }

        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];

        final double magnitudeSquared = ax * ax + ay * ay + az * az;
        int accelerationSquared = ACCELERATION_THREASHOLD * ACCELERATION_THREASHOLD;
        if (magnitudeSquared > accelerationSquared && mShakeListener != null) {
            mLastShakeTime = curTime;
            mShakeListener.execute();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }
}
