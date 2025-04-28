package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class Screen11 : AppCompatActivity() {
    private val userList = mutableListOf<User>()
    private lateinit var adapter: FollowAdapter
    private lateinit var queue: com.android.volley.RequestQueue
    private val baseUrl = "http://192.168.100.40/assignment3_backend/"
    private val userId: String by lazy {
        getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen12)

        queue = Volley.newRequestQueue(this)

        val currentUser = findViewById<TextView>(R.id.user_name)
        currentUser.text = intent.getStringExtra("userName") ?: "User"

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_follow)
        adapter = FollowAdapter(userList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
            return
        }

        fetchUserStats()
        fetchFollowers()

        val backIcon = findViewById<ImageView>(R.id.back_button)
        backIcon.setOnClickListener {
            finish()
        }

        val followersCount = findViewById<TextView>(R.id.followers_count)
        followersCount.setOnClickListener {
            finish()
        }

        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnEditorActionListener { _, _, _ ->
            val query = searchBar.text.toString().trim()
            if (query.isNotEmpty()) {
                searchFollowers(query)
            } else {
                fetchFollowers()
            }
            true
        }
    }

    private fun fetchUserStats() {
        val url = "${baseUrl}get_user_stats.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val followers = response.getInt("followers")
                    val following = response.getInt("following")
                    findViewById<TextView>(R.id.followers_count).text = "$followers Followers"
                    findViewById<TextView>(R.id.following_count).text = "$following Following"
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching stats: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun fetchFollowers() {
        val url = "${baseUrl}get_followers.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("Screen12", "Followers response: $response")
                if (response.getString("status") == "success") {
                    userList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        userList.add(
                            User(
                                userId = user.getString("user_id"),
                                userName = user.getString("username"),
                                name = user.getString("name"),
                                profileImageUrl = user.getString("profile_image")
                            )
                        )
                    }
                    Log.d("Screen12", "Followers list size: ${userList.size}")
                    if (userList.isEmpty()) {
                        Toast.makeText(this, "No users found!", Toast.LENGTH_SHORT).show()
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching followers: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun searchFollowers(query: String) {
        val url = "${baseUrl}search_follow.php?user_id=$userId&type=followers&query=$query"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("Screen12", "Search followers response: $response")
                if (response.getString("status") == "success") {
                    userList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        userList.add(
                            User(
                                userId = user.getString("user_id"),
                                userName = user.getString("username"),
                                name = user.getString("name"),
                                profileImageUrl = user.getString("profile_image")
                            )
                        )
                    }
                    Log.d("Screen12", "Search followers list size: ${userList.size}")
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error searching followers: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}