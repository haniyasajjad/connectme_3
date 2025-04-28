package com.fast.assignment3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
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
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class Screen9 : AppCompatActivity() {
    private lateinit var recipientUserId: String
    private lateinit var rtcEngine: RtcEngine
    private val channelName = "haniya"
    private val agoraToken = "007eJxTYEjlOLlbwFGidVW9ZrR4rqvc2qyod0xOgjkTmGxkY+8uKFZgMEgxTrJMNTA2MTQ3MTFPSbY0TTNNTUwzMzc0TLFMSzEIkOLLaAhkZOifIsvEyACBID4bQ0ZiXmZlIgMDABwXGx0="
    private val appId = "0d3b9e03417447dc95f5eaf6711d9fd0"
    private var seconds = 0
    private var isMuted = false
    private var isVideoEnabled = true
    private var isSpeakerOn = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen9)
        recipientUserId = intent.getStringExtra("userId") ?: ""
        val userNameTextView: TextView = findViewById(R.id.user_name)
        fetchRecipientName(recipientUserId, userNameTextView)

        val videoOff = findViewById<ImageView>(R.id.video_call_icon)
        val muteButton = findViewById<ImageView>(R.id.mute_button)
        val speakerButton = findViewById<ImageView>(R.id.speaker_button)
        val endCall = findViewById<ImageView>(R.id.end_call_button)

        videoOff.setOnClickListener {
            isVideoEnabled = !isVideoEnabled
            rtcEngine.enableLocalVideo(isVideoEnabled)
            videoOff.setImageResource(if (isVideoEnabled) R.drawable.ic_video_call else R.drawable.ic_video_call_s6)
        }

        muteButton.setOnClickListener {
            isMuted = !isMuted
            rtcEngine.muteLocalAudioStream(isMuted)
            muteButton.setImageResource(if (isMuted) R.drawable.ic_mute else R.drawable.ic_mute)
        }

        speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            rtcEngine.setEnableSpeakerphone(isSpeakerOn)
            speakerButton.setImageResource(if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker)
        }

        endCall.setOnClickListener {
            rtcEngine.leaveChannel()
            rtcEngine.stopPreview()
            RtcEngine.destroy()
            handler.removeCallbacks(timerRunnable)
            finish()
        }

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                100
            )
        } else {
            initializeAndJoinVideoChannel()
            setupLocalVideo()
            startTimer()
        }
    }

    private fun checkPermissions(): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val cameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return audioPermission && cameraPermission
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeAndJoinVideoChannel()
            setupLocalVideo()
            startTimer()
        } else {
            Toast.makeText(this, "Audio and camera permissions are required", Toast.LENGTH_LONG).show()
            finish()
        }
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

    private fun initializeAndJoinVideoChannel() {
        try {
            rtcEngine = RtcEngine.create(baseContext, appId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.i("Agora", "Joined video channel $channel with uid $uid")
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.i("Agora", "Remote user joined video with uid $uid")
                    runOnUiThread { setupRemoteVideo(uid) }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.i("Agora", "Remote video user offline with uid $uid")
                    runOnUiThread { findViewById<FrameLayout>(R.id.remote_video_container)?.removeAllViews() }
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
                        Toast.makeText(this@Screen9, "Error: $err ($errorMessage)", Toast.LENGTH_LONG).show()
                    }
                }
            })

            rtcEngine.enableVideo()
            rtcEngine.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
            rtcEngine.joinChannel(agoraToken, channelName, "Extra Optional Data", 0)
        } catch (e: Exception) {
            Log.e("Agora", "Error initializing Agora Video: ${e.message}")
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocalVideo() {
        val localSurfaceView = SurfaceView(this)
        localSurfaceView.setZOrderMediaOverlay(true)
        findViewById<FrameLayout>(R.id.local_video_container)?.addView(localSurfaceView)
        rtcEngine.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        rtcEngine.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        val remoteSurfaceView = SurfaceView(this)
        remoteSurfaceView.setZOrderMediaOverlay(true)
        findViewById<FrameLayout>(R.id.remote_video_container)?.addView(remoteSurfaceView)
        rtcEngine.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
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