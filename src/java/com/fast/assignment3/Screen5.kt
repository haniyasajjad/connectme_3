package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class Screen5 : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private lateinit var queue: com.android.volley.RequestQueue
    private val userId: String by lazy {
        getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen5)

        queue = Volley.newRequestQueue(this)

        recyclerView = findViewById(R.id.recycler_users)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val goToRequests = findViewById<TextView>(R.id.tab_requests)
        val profileNameTextView = findViewById<TextView>(R.id.profile_name)

        userAdapter = UserAdapter(userList) { selectedUser ->
            val intent = Intent(this, Screen6::class.java)
            intent.putExtra("userId", selectedUser.userId)
            intent.putExtra("userName", selectedUser.userName)
            intent.putExtra("name", selectedUser.name)
            intent.putExtra("profileImageUrl", selectedUser.profileImageUrl)
            startActivity(intent)
        }
        recyclerView.adapter = userAdapter

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
            return
        }

        fetchCurrentUserName(profileNameTextView)
        fetchUsers()

        /*goToRequests.setOnClickListener {
            startActivity(Intent(this, Screen19::class.java))
        }*/

        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }
    }

    private fun fetchUsers() {
        val url = "http://192.168.100.40/assignment3_backend/get_users.php?current_user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    userList.clear()
                    val usersArray = response.getJSONArray("users")
                    for (i in 0 until usersArray.length()) {
                        val userJson = usersArray.getJSONObject(i)
                        userList.add(
                            User(
                                userId = userJson.getString("userId"),
                                userName = userJson.getString("userName"),
                                name = userJson.getString("name"),
                                profileImageUrl = userJson.getString("profileImageUrl")
                            )
                        )
                    }
                    if (userList.isEmpty()) {
                        Toast.makeText(this, "No users found!", Toast.LENGTH_SHORT).show()
                    }
                    userAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun fetchCurrentUserName(profileNameTextView: TextView) {
        val url = "http://192.168.100.40/assignment3_backend/get_user.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    val userJson = response.getJSONObject("user")
                    val name = userJson.getString("Name") ?: "User"
                    profileNameTextView.text = name
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}