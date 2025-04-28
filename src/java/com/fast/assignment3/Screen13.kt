package com.fast.assignment3

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Response
import com.android.volley.Request
import com.fast.assignment3.R
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class Screen13 : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var userId: Int = -1

    private val TAG = "RegisterDebug"
    private val baseUrl = "http://192.168.100.40/assignment3_backend/"
    private val getUserUrl = "${baseUrl}getUser.php"
    private val updateUserUrl = "${baseUrl}updateUser.php"
    private val updateImageUrl = "${baseUrl}updateImage.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen13)

        profileImageView = findViewById(R.id.profile_image)
        val name = intent.getStringExtra("name") ?: ""
        findViewById<EditText>(R.id.name_field).setText(name)
        val username = intent.getStringExtra("username") ?: ""
        findViewById<EditText>(R.id.username_field).setText(username)



        fetchUserData(username)

        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        findViewById<TextView>(R.id.done_button).setOnClickListener {
            saveProfileData()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImageView.setImageBitmap(bitmap)

                val base64Image = encodeImageToBase64(bitmap)
                uploadProfileImageToServer(base64Image)
            }
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun fetchUserData(username: String) {
        val queue = Volley.newRequestQueue(this)


        val request = object : StringRequest(
            Method.POST, getUserUrl,
            Response.Listener { response ->
                Log.d(TAG, "Raw response: $response")
                try {
                    val json = JSONObject(response)
                    val name = json.getString("Name")
                    val contact = json.getString("Phone")
                    val bio = json.getString("Bio")
                    val base64Image = json.getString("Profileimage")
                    val userId = json.getInt("ID")


                    // Pass ID to the next screen (or store it for later use)
                    this.userId = userId


                    findViewById<TextView>(R.id.namedisplay).text = name
                    findViewById<EditText>(R.id.name_field).setText(name)
                    findViewById<EditText>(R.id.username_field).setText(username)
                    findViewById<EditText>(R.id.contact_field).setText(contact)
                    findViewById<EditText>(R.id.bio_field).setText(bio)

                    if (!base64Image.isNullOrEmpty() && base64Image != "null") {
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profileImageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Parsing error: ${e.message}", e)
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error fetching user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("username" to username)
            }
        }

        queue.add(request)
    }


    private fun saveProfileData() {
        val name = findViewById<EditText>(R.id.name_field).text.toString()
        val username = findViewById<EditText>(R.id.username_field).text.toString()
        val phone = findViewById<EditText>(R.id.contact_field).text.toString()
        val bio = findViewById<EditText>(R.id.bio_field).text.toString()

        val queue = Volley.newRequestQueue(this)
        Log.d("UpdateUser", "Name: $name, Username: $username, Bio: $bio")

        val request = object : StringRequest(
            Method.POST, updateUserUrl,
            Response.Listener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                //startActivity(Intent(this, Screen4::class.java))
                finish()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Update failed: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Update failed: ${error.message}", error)
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "name" to name,
                    "username" to username,
                    "phone" to phone,
                    "bio" to bio
                )
            }
        }

        queue.add(request)
    }

    private fun uploadProfileImageToServer(base64Image: String) {
        val username = findViewById<EditText>(R.id.username_field).text.toString()
        val userId = this.userId
        // Get the ID passed via the Intent (ensure this is set in the previous screen)

        if (userId == -1) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(
            Method.POST, updateImageUrl,
            Response.Listener {
                Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Image upload failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "username" to username,
                    "profileImage" to base64Image,
                    "ID" to userId.toString() // Add ID here
                )
            }
        }

        queue.add(request)
    }

}