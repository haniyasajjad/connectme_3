package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class Screen18 : AppCompatActivity() {
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var inviteAdapter: InviteAdapter
    private val contactsList = mutableListOf<Contact>()
    private val invitesList = mutableListOf<Contact>()
    private var currentUserId: String = ""
    private val baseUrl = "http://192.168.100.40/assignment3_backend/"
    private lateinit var queue: com.android.volley.RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen18)

        queue = Volley.newRequestQueue(this)

        fetchCurrentUserId()

        val profileNameTextView: TextView = findViewById(R.id.profile_name)
        fetchCurrentUserName(profileNameTextView)

        setupRecyclerView(
            findViewById(R.id.recycler_contacts),
            LinearLayoutManager(this),
            ContactsAdapter(contactsList) { contact, action ->
                handleFriendRequest(contact, action)
            }.also { contactsAdapter = it }
        )

        setupRecyclerView(
            findViewById(R.id.recycler_new_friends),
            LinearLayoutManager(this),
            InviteAdapter(invitesList) { contact ->
                sendFriendRequest(contact)
            }.also { inviteAdapter = it }
        )

        val backIcon: ImageView = findViewById(R.id.back_icon)
        backIcon.setOnClickListener { finish() }

        val editIcon: ImageView = findViewById(R.id.edit_icon)
        /*editIcon.setOnClickListener {
            startActivity(Intent(this, Screen19::class.java))
        }*/

        val searchBar: EditText = findViewById(R.id.search_bar)
        searchBar.setOnEditorActionListener { _, _, _ ->
            val query = searchBar.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
            }
            true
        }

        /*val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_followers -> {
                    showFollowers()
                    true
                }
                R.id.nav_following -> {
                    showFollowing()
                    true
                }
                else -> false
            }
        }*/

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateFCMToken(task.result)
                } else {
                    Log.e("FCM", "Failed to retrieve FCM token: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "FCM token fetch failed: ${e.message}")
        }
    }

    private fun fetchCurrentUserId() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
        } else {
            fetchFriendRequests()
            fetchSuggestedUsers()
        }
    }

    private fun fetchCurrentUserName(profileNameTextView: TextView) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}get_user.php?user_id=$currentUserId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val user = response.getJSONObject("user")
                    profileNameTextView.text = user.getString("Name") ?: "User"
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

    private fun fetchFriendRequests() {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}get_friend_requests.php?user_id=$currentUserId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    contactsList.clear()
                    val requests = response.getJSONArray("requests")
                    for (i in 0 until requests.length()) {
                        val req = requests.getJSONObject(i)
                        contactsList.add(
                            Contact(
                                userId = req.getString("user_id"),
                                profileImage = req.getString("profile_image"),
                                name = req.getString("name"),
                                status = req.getString("status")
                            )
                        )
                    }
                    Log.d("Screen18", "Friend requests size: ${contactsList.size}")
                    contactsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching friend requests: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun fetchSuggestedUsers() {
        if (currentUserId.isEmpty()) return
        Log.d("currentUserId", " $currentUserId")

        val url = "${baseUrl}get_suggested_users.php?user_id=$currentUserId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("Screen18", "Suggested users response: $response")
                if (response.getString("status") == "success") {
                    invitesList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        invitesList.add(
                            Contact(
                                userId = user.getString("user_id"),
                                profileImage = user.getString("profile_image"),
                                name = user.getString("name"),
                                status = user.getString("status")
                            )
                        )
                    }
                    Log.d("Screen18", "Invites list size: ${invitesList.size}")
                    if (invitesList.isEmpty()) {
                        Toast.makeText(this, "No suggested users found!", Toast.LENGTH_SHORT).show()
                    }
                    inviteAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching suggested users: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun searchUsers(query: String) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}search_users.php?query=$query&user_id=$currentUserId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    invitesList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        invitesList.add(
                            Contact(
                                userId = user.getString("user_id"),
                                profileImage = user.getString("profile_image"),
                                name = user.getString("name"),
                                status = user.getString("status")
                            )
                        )
                    }
                    Log.d("Screen18", "Search results size: ${invitesList.size}")
                    inviteAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error searching users: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun sendFriendRequest(contact: Contact) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}send_friend_request.php"
        val json = JSONObject().apply {
            put("sender_id", currentUserId.toIntOrNull() ?: return@apply)
            put("receiver_id", contact.userId.toIntOrNull() ?: return@apply)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.getString("status") == "success") {
                    val senderName = response.getString("sender_name")
                    val index = invitesList.indexOf(contact)
                    if (index != -1) {
                        invitesList[index] = contact.copy(status = "sent")
                        inviteAdapter.notifyItemChanged(index)
                    }
                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show()
                    sendNotificationToUser(contact.userId.toIntOrNull() ?: return@JsonObjectRequest, "$senderName sent you a follow request")
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error sending friend request: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun handleFriendRequest(contact: Contact, action: String) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}handle_friend_request.php"
        val json = JSONObject().apply {
            put("receiver_id", currentUserId.toIntOrNull() ?: return@apply)
            put("sender_id", contact.userId.toIntOrNull() ?: return@apply)
            put("action", action)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.getString("status") == "success") {
                    contactsList.remove(contact)
                    contactsAdapter.notifyDataSetChanged()
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error handling friend request: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun showFollowers() {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}get_followers_following.php?user_id=$currentUserId&type=followers"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    contactsList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        contactsList.add(
                            Contact(
                                userId = user.getString("user_id"),
                                profileImage = user.getString("profile_image"),
                                name = user.getString("name"),
                                status = "none"
                            )
                        )
                    }
                    Log.d("Screen18", "Followers size: ${contactsList.size}")
                    contactsAdapter.notifyDataSetChanged()
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

    private fun showFollowing() {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}get_followers_following.php?user_id=$currentUserId&type=following"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    contactsList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        contactsList.add(
                            Contact(
                                userId = user.getString("user_id"),
                                profileImage = user.getString("profile_image"),
                                name = user.getString("name"),
                                status = "none"
                            )
                        )
                    }
                    Log.d("Screen18", "Following size: ${contactsList.size}")
                    contactsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error fetching following: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun sendNotificationToUser(userId: Int, message: String) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}send_notification.php"
        val json = JSONObject().apply {
            put("sender_id", currentUserId.toIntOrNull() ?: return@apply)
            put("receiver_id", userId)
            put("message", message)
            put("type", "new_follow")
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.getString("status") == "success") {
                    Log.d("OneSignal", "Follow notification sent successfully")
                } else {
                    Log.e("OneSignal", "Failed to send follow notification: ${response.getString("message")}")
                }
            },
            { error ->
                Log.e("OneSignal", "Error sending follow notification: ${error.message}")
            }
        )
        queue.add(request)
    }

    private fun updateFCMToken(token: String) {
        if (currentUserId.isEmpty()) return

        val url = "${baseUrl}update_profile.php"
        val json = JSONObject().apply {
            put("user_id", currentUserId)
            put("fcm_token", token)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.getString("status") == "success") {
                    Log.d("FCM", "FCM token updated")
                }
            },
            { error ->
                Log.e("FCM", "Error updating FCM token: ${error.message}")
            }
        )
        queue.add(request)
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        layoutManager: RecyclerView.LayoutManager,
        adapter: RecyclerView.Adapter<*>
    ) {
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }
}