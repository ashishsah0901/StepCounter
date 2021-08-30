package com.example.stepcounter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.stepcounter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isTracking = TrackingService.isTracking.value == true
        setUpWalkingState()
        if(!(ContextCompat.checkSelfPermission(this,
                        Manifest.permission.FOREGROUND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(
                                Manifest.permission.ACTIVITY_RECOGNITION,
                                Manifest.permission.ACTIVITY_RECOGNITION
                        ),10
                )
            }
        }
        binding.startOrStop.setOnClickListener {
            if(!isTracking) {
                startService()
            }else{
                stopService()
            }
        }
        binding.buttonReset.setOnClickListener {
            if(isTracking){
                Toast.makeText(this,"Please stop your walk before resetting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetUI()
        }
    }

    private fun resetUI() {
        binding.resultMessage.text = "Want to count steps\nClick on start"
        binding.numberOfSteps.text = "0"
    }

    private fun startService() {
        isTracking = true
        binding.resultMessage.text = "Come On\nKeep Walking..."
        Intent(this, TrackingService::class.java).apply {
            this.action = Constants.START_SERVICE
            startService(this)
        }
    }

    private fun stopService() {
        isTracking = false
        Intent(this, TrackingService::class.java).apply {
            this.action = Constants.STOP_SERVICE
            startService(this)
        }
    }

    private fun setUpWalkingState() {
        TrackingService.isTracking.observe(this){
            if(it) {
                binding.startOrStop.text = "Stop"
            } else {
                binding.startOrStop.text = "Start"
                updateUI()
            }
        }
        TrackingService.numberOfStepsLiveData.observe(this){
            binding.numberOfSteps.text = it.toString()
        }
    }

    private fun updateUI() {
        if(TrackingService.numberOfSteps >= 1000) {
            binding.resultMessage.text = "Congratulation\n Great Walk"
        }else{
            binding.resultMessage.text = "Poor Performance\n Disappointing"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Intent(this, TrackingService::class.java).apply {
            this.action = Constants.STOP_SERVICE
            startService(this)
        }
    }

}