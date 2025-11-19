package com.example.network.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Interface to deliver the shake event back to the activity
interface OnShakeListener {
    fun onShake()
}

class ShakeDetector(private val listener: OnShakeListener) : SensorEventListener {

    // Thresholds for shake detection
    private val SHAKE_THRESHOLD_GRAVITY = 1.2f
    private val SHAKE_SLOP_TIME_MS = 500
    private val SHAKE_COUNT_RESET_MS = 3000

    private var mShakeTimestamp: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for accelerometer
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate g-force total
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // Ignore shakes that are too close in time
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                mShakeTimestamp = now

                // Trigger the shake event
                listener.onShake()
            }
        }
    }
}