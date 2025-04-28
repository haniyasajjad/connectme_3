package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView

class Screen10 : AppCompatActivity() {
    private val postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostFeedAdapter
    private lateinit var queue: com.android.volley.RequestQueue
    private val baseUrl = "http://192.168.100.40/assignment3_backend/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen10)

        queue = Volley.newRequestQueue(this)

        // Get userId from Intent or SharedPreferences
        val userId = intent.getIntExtra("selected_user_id", -1).takeIf { it != -1 }?.toString()
            ?: getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
            return
        }

        val profileNameTextView = findViewById<TextView>(R.id.user_name)

        fetchUserProfile(profileNameTextView, userId)
        fetchUserStats(userId)

        postAdapter = PostFeedAdapter(postList)
        setupRecyclerView(
            findViewById(R.id.recycler_feed),
            GridLayoutManager(this, 3),
            postAdapter
        )
        fetchUserPosts(userId)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
                else -> false
            }
        }

        val gotoFollowers = findViewById<LinearLayout>(R.id.followers)
        gotoFollowers.setOnClickListener {
            val intent = Intent(this, Screen11::class.java)
            intent.putExtra("userName", profileNameTextView.text)
            startActivity(intent)
        }

        val gotoFollowing = findViewById<LinearLayout>(R.id.following)
        gotoFollowing.setOnClickListener {
            val intent = Intent(this, Screen12::class.java)
            intent.putExtra("userName", profileNameTextView.text)
            startActivity(intent)
        }

        val editProfile = findViewById<ImageView>(R.id.penic)
        editProfile.setOnClickListener {
            val intent = Intent(this, Screen13::class.java)
            intent.putExtra("userName", profileNameTextView.text)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        layoutManager: RecyclerView.LayoutManager,
        adapter: RecyclerView.Adapter<*>
    ) {
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun fetchUserProfile(profileNameTextView: TextView, userId: String) {
        val url = "${baseUrl}get_user_profile.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("Screen10", "Profile response: $response")
                if (response.getString("status") == "success") {
                    val user = response.getJSONObject("user")
                    profileNameTextView.text = user.getString("name")
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching profile: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun fetchUserStats(userId: String) {
        val url = "${baseUrl}get_user_stats.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val followers = response.getInt("followers")
                    val following = response.getInt("following")
                    //findViewById<TextView>(R.id.followers_count).text = "$followers Followers"
                    //findViewById<TextView>(R.id.following_count).text = "$following Following"
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

    private fun fetchUserPosts(userId: String) {
        val url = "${baseUrl}get_user_posts.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("Screen10", "Posts response: $response")
                if (response.getString("status") == "success") {
                    postList.clear()
                    val posts = response.getJSONArray("posts")
                    for (i in 0 until posts.length()) {
                        val post = posts.getJSONObject(i)
                        postList.add(
                            Post(
                                postId = post.getString("post_id"),
                                userId = post.getString("user_id"),
                                caption = post.getString("caption"),
                                media = post.getString("media"),
                                likes = post.getLong("likes"),
                                timestamp = post.getLong("timestamp")
                            )
                        )
                    }
                    Log.d("Screen10", "Posts list size: ${postList.size}")
                    if (postList.isEmpty()) {
                        Toast.makeText(this, "No posts found!", Toast.LENGTH_SHORT).show()
                    }
                    postAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching posts: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}