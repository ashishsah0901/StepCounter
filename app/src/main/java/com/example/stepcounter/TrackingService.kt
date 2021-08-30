package com.example.stepcounter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.stepcounter.Constants.NOTIFICATION_CHANNEL_ID
import com.example.stepcounter.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.stepcounter.Constants.NOTIFICATION_ID
import com.example.stepcounter.Constants.START_SERVICE
import com.example.stepcounter.Constants.STOP_SERVICE

class TrackingService: LifecycleService(), SensorEventListener {

    companion object{
        val numberOfStepsLiveData = MutableLiveData<Int>()
        val isTracking = MutableLiveData<Boolean>()
        var numberOfSteps: Int = 0
    }

    private var initialSteps = 0
    private var isFirstData = true

    private var serviceKilled = true

    private var mSensorManager: SensorManager? = null
    private var mStepDetectorSensor: Sensor? = null

    private lateinit var curNotificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        mSensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if(mSensorManager == null){
            Toast.makeText(this, "Your Device doesn't have sensor to count steps", Toast.LENGTH_LONG).show()
            stopSelf()
        }
        numberOfSteps = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                START_SERVICE -> {
                    isFirstData = true
                    isTracking.postValue(true)
                    serviceKilled = false
                    startForegroundService()
                }
                STOP_SERVICE -> {
                    mSensorManager!!.unregisterListener(this)
                    isTracking.postValue(false)
                    serviceKilled = true
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isFirstData && initialSteps==0){
            initialSteps = event?.values?.get(0)?.toInt()!!
            isFirstData = false
        }
        numberOfSteps = event?.values?.get(0)?.toInt()!! - initialSteps
        numberOfStepsLiveData.postValue(numberOfSteps)
        updateNotification(numberOfSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun startForegroundService(){
        if (mSensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            mStepDetectorSensor =
                    mSensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            mSensorManager!!.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }
        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java)
                        .apply {
                            putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        curNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_run)
                .setContentTitle("Counting Steps...")
                .setContentText("0")
                .setContentIntent(pendingIntent)
        startForeground(NOTIFICATION_ID, curNotificationBuilder.build())

        numberOfStepsLiveData.observe(this, {
            updateNotification(it)
        })
    }

    private fun updateNotification(count: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(!serviceKilled){
            curNotificationBuilder = curNotificationBuilder
                    .setContentText(count.toString())
            notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}