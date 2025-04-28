package com.fast.assignment3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp

class Screen1 : AppCompatActivity() {
    companion object {
        const val TAG = "FCM"
        private const val REQUEST_CODE_NOTIFICATIONS = 1001

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen1)
        FirebaseApp.initializeApp(this)
        requestNotificationPermission()
        val areEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d("OneSignalDebug", "Are notifications enabled: $areEnabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATIONS)
            }
        }


        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Screen2::class.java)
            startActivity(intent)
            finish() // Close Splash Screen
        }, 9000) // 5 seconds delay
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted")
            } else {
                Log.w(TAG, "Notification permission denied")
            }
        }
    }

    private fun requestNotificationPermission() {
        // Only request permission on Android 13 (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Permission already granted, notifications can be displayed
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Below Android 13, no runtime permission needed
            Toast.makeText(this, "Notifications enabled (no permission needed)", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, notifications can be displayed
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied, inform user or disable notifications
            Toast.makeText(
                this,
                "Notification permission denied. Enable it in settings to receive notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


}
