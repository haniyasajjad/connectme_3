package com.fast.assignment3


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import org.json.JSONObject

class Screen8 : AppCompatActivity() {
    private lateinit var recipientUserId: String
    private var mRtcEngine: RtcEngine? = null
    private var isMuted = false
    private var isSpeakerOn = true
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )
    private val PERMISSION_REQUEST_CODE = 1
    private val channelName = "haniya"
    private val agoraToken = "007eJxTYEjlOLlbwFGidVW9ZrR4rqvc2qyod0xOgjkTmGxkY+8uKFZgMEgxTrJMNTA2MTQ3MTFPSbY0TTNNTUwzMzc0TLFMSzEIkOLLaAhkZOifIsvEyACBID4bQ0ZiXmZlIgMDABwXGx0="
    private val appId = "0d3b9e03417447dc95f5eaf6711d9fd0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen8)

        recipientUserId = intent.getStringExtra("userId") ?: ""
        val userNameTextView: TextView = findViewById(R.id.user_name)
        val videoOn = findViewById<ImageView>(R.id.video_call_icon)
        val muteButton = findViewById<ImageView>(R.id.mute_button)
        val speakerButton = findViewById<ImageView>(R.id.speaker_button)
        val endCall = findViewById<ImageView>(R.id.end_call_button)

        fetchRecipientName(recipientUserId, userNameTextView)

        videoOn.setOnClickListener {
            mRtcEngine?.leaveChannel()
            val intent = Intent(this, Screen9::class.java)
            intent.putExtra("userId", recipientUserId)
            startActivity(intent)
        }

        muteButton.setOnClickListener {
            isMuted = !isMuted
            mRtcEngine?.muteLocalAudioStream(isMuted)
            muteButton.setImageResource(if (isMuted) R.drawable.ic_mute else R.drawable.ic_mute)
        }

        speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            mRtcEngine?.setEnableSpeakerphone(isSpeakerOn)
            speakerButton.setImageResource(if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker)
        }

        endCall.setOnClickListener {
            finishCall()
        }

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            initializeAndJoinChannel()
            startTimer()
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAndJoinChannel()
                startTimer()
            } else {
                Toast.makeText(this, "Audio permission is required for voice calls", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, appId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.i("Agora", "Joined channel $channel with uid $uid")
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.i("Agora", "Remote user joined with uid $uid")
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.i("Agora", "Remote user offline with uid $uid")
                }

                override fun onError(err: Int) {
                    val errorMessage = when (err) {
                        101 -> "Invalid App ID"
                        109 -> "Token Expired"
                        110 -> "Invalid Token"
                        else -> "Unknown Error: $err"
                    }
                    Log.e("Agora", "Agora error: $err - $errorMessage")
                    runOnUiThread {
                        Toast.makeText(this@Screen8, "Call error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            })

            mRtcEngine?.enableAudio()
            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
            mRtcEngine?.joinChannel(agoraToken, channelName, "Extra Optional Data", 0)
        } catch (e: Exception) {
            Log.e("Agora", "Error initializing Agora RtcEngine: ${e.message}")
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun finishCall() {
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        handler.removeCallbacks(timerRunnable)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mRtcEngine != null) {
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
        }
        handler.removeCallbacks(timerRunnable)
    }

    private fun fetchRecipientName(userId: String, userNameTextView: TextView) {
        val apiUrl = "http://192.168.100.40/assignment3_backend/get_user.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                try {
                    val status = response.getString("status")
                    if (status == "success") {
                        val user = response.getJSONObject("user")
                        val name = user.getString("Name") ?: "Unknown"
                        userNameTextView.text = name
                    } else {
                        Log.e("Volley", "User not found: ${response.getString("message")}")
                        userNameTextView.text = "Unknown"
                    }
                } catch (e: Exception) {
                    Log.e("Volley", "Error parsing response: ${e.message}")
                    userNameTextView.text = "Unknown"
                }
            },
            { error ->
                Log.e("Volley", "Error fetching user data: ${error.message}")
                userNameTextView.text = "Unknown"
            }
        )
        queue.add(jsonObjectRequest)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            val minutes = seconds / 60
            val sec = seconds % 60
            findViewById<TextView>(R.id.call_duration).text = String.format("%02d:%02d", minutes, sec)
            handler.postDelayed(this, 1000)
        }
    }

    private fun startTimer() {
        handler.post(timerRunnable)
    }
}