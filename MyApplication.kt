package com.fast.assignment3

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d("MyApplication", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Firebase initialization failed: ${e.message}")
        }
    }
}