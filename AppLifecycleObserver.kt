package com.fast.assignment3

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class AppLifecycleObserver(private val lifecycle: Lifecycle, private val context: Context) : DefaultLifecycleObserver {

    private val queue = Volley.newRequestQueue(context)
    private var isLoggedIn = true

    fun onLogout() {
        isLoggedIn = false
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isLoggedIn) {
            setUserOnline()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (isLoggedIn) {
            setUserOffline()
        }
    }

    private fun setUserOnline() {
        val userId = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", "") ?: return
        if (userId.isEmpty()) {
            Log.e("Lifecycle", "User ID is empty in setUserOnline")
            return
        }

        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("online_status", true)
            put("last_online", System.currentTimeMillis())
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/update_status.php",
            jsonBody,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        Log.d("Lifecycle", "User set online")
                    } else {
                        Log.e("Lifecycle", "Error setting user online: ${response.getString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("Lifecycle", "JSON parse error in setUserOnline: ${e.message}, Response: $response")
                }
            },
            { error ->
                Log.e("Lifecycle", "Volley error in setUserOnline: ${error.message}, Network response: ${error.networkResponse?.statusCode}")
            }
        )
        queue.add(request)
    }

    private fun setUserOffline() {
        val userId = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", "") ?: return
        if (userId.isEmpty()) {
            Log.e("Lifecycle", "User ID is empty in setUserOffline")
            return
        }

        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("online_status", false)
            put("last_online", System.currentTimeMillis())
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/update_status.php",
            jsonBody,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        Log.d("Lifecycle", "User set offline")
                    } else {
                        Log.e("Lifecycle", "Error setting user offline: ${response.getString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("Lifecycle", "JSON parse error in setUserOffline: ${e.message}, Response: $response")
                }
            },
            { error ->
                Log.e("Lifecycle", "Volley error in setUserOffline: ${error.message}, Network response: ${error.networkResponse?.statusCode}")
            }
        )
        queue.add(request)
    }
}