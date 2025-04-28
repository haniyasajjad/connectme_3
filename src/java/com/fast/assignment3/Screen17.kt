package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.UUID

class Screen17 : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageGalleryAdapter
    private val imageList = mutableListOf<String>()
    private lateinit var queue: com.android.volley.RequestQueue
    private val userId: String by lazy {
        getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen17)

        queue = Volley.newRequestQueue(this)

        recyclerView = findViewById(R.id.image_gallery)
        val captionInput = findViewById<EditText>(R.id.caption_input)
        val shareButton = findViewById<Button>(R.id.share_button)
        val backButton = findViewById<ImageView>(R.id.back_button)

        // Receive selected image from Screen16
        val selectedImage = intent.getStringExtra("selectedImage")

        // Add selected image to the list as the first item
        selectedImage?.let {
            imageList.add(0, it)
        }

        // Initialize RecyclerView
        imageAdapter = ImageGalleryAdapter(imageList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = imageAdapter

        shareButton.setOnClickListener {
            val caption = captionInput.text.toString().trim()
            if (caption.isEmpty()) {
                Toast.makeText(this, "Caption cannot be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (imageList.isEmpty()) {
                Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (userId.isEmpty()) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Screen2::class.java))
                finish()
                return@setOnClickListener
            }

            uploadPost(caption, imageList[0])
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun uploadPost(caption: String, base64Image: String) {
        val postId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val jsonBody = JSONObject().apply {
            put("post_id", postId)
            put("user_id", userId)
            put("caption", caption)
            put("media", base64Image)
            put("timestamp", timestamp)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/upload_post.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    // Store post locally
                    val dbHelper = DatabaseHelper(this)
                    dbHelper.insertPost(
                        LocalPost(
                            postId = postId,
                            userId = userId,
                            caption = caption,
                            media = base64Image,
                            likes = 0,
                            timestamp = timestamp
                        )
                    )
                    Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Screen4::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Failed to post: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to post: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}