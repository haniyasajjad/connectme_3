package com.fast.assignment3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class Screen14 : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchAdapter: RecentSearchAdapter
    private val userList = mutableListOf<Pair<Int, String>>() // Store (userId, username)
    private val filteredList = mutableListOf<Pair<Int, String>>() // Store search results
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private lateinit var filterDropdown: Spinner
    private lateinit var queue: com.android.volley.RequestQueue
    private val baseUrl = "http://192.168.100.40/assignment3_backend/"
    private var currentUserDbId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen14)

        queue = Volley.newRequestQueue(this)

        searchEditText = findViewById(R.id.search_bar_input)
        recyclerView = findViewById(R.id.recycler_recent_searches)

        recyclerView.layoutManager = LinearLayoutManager(this)
        searchAdapter = RecentSearchAdapter(filteredList, { search ->
            // Handle cross button click
            filteredList.remove(search)
            searchAdapter.notifyDataSetChanged()
        }, { userId ->
            // Handle item click to open profile
            val intent = Intent(this, Screen10::class.java)
            intent.putExtra("selected_user_id", userId)
            startActivity(intent)
        })
        recyclerView.adapter = searchAdapter

        /*if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
            return
        }*/

        fetchCurrentUserDbId()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        filterDropdown = findViewById(R.id.filter_dropdown)

        val filterOptions = arrayOf("All", "Followers", "Following")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterDropdown.adapter = adapter

        filterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadAllUsers()
                    1 -> loadFilteredUsers("followers")
                    2 -> loadFilteredUsers("following")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Screen4::class.java))
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, Screen18::class.java))
                    true
                }
                R.id.nav_search -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchCurrentUserDbId() {
        val url = "${baseUrl}get_user_id.php?firebase_uid=$currentUserId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    currentUserDbId = response.getInt("user_id")
                    loadAllUsers()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                    /*startActivity(Intent(this, Screen2::class.java))
                    finish()*/
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                //startActivity(Intent(this, Screen2::class.java))
                //finish()
            }
        )
        queue.add(request)
    }

    private fun loadAllUsers() {
        /*if (currentUserDbId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }*/
        val url = "${baseUrl}get_users.php?current_user_id=$currentUserDbId"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    userList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        userList.add(Pair(user.getInt("userId"), user.getString("userName")))
                    }
                    filteredList.clear()
                    filteredList.addAll(userList)
                    searchAdapter.notifyDataSetChanged()
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

    private fun filterUsers(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(userList)
        } else {
            filteredList.addAll(userList.filter { it.second.contains(query, ignoreCase = true) })
        }
        searchAdapter.notifyDataSetChanged()
    }

    private fun loadFilteredUsers(type: String) {
        /*if (currentUserDbId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }*/
        val url = "${baseUrl}get_followers_following.php?user_id=$currentUserDbId&type=$type"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val userIds = response.getJSONArray("userIds")
                    val ids = mutableListOf<Int>()
                    for (i in 0 until userIds.length()) {
                        ids.add(userIds.getInt(i))
                    }
                    if (ids.isEmpty()) {
                        filteredList.clear()
                        searchAdapter.notifyDataSetChanged()
                        return@JsonObjectRequest
                    }
                    fetchUsernamesByIds(ids)
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

    private fun fetchUsernamesByIds(userIds: List<Int>) {
        val url = "${baseUrl}get_usernames_by_ids.php"
        val jsonObject = JSONObject().apply {
            put("user_ids", userIds.joinToString(","))
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.getString("status") == "success") {
                    filteredList.clear()
                    val users = response.getJSONArray("users")
                    for (i in 0 until users.length()) {
                        val user = users.getJSONObject(i)
                        filteredList.add(Pair(user.getInt("userId"), user.getString("userName")))
                    }
                    searchAdapter.notifyDataSetChanged()
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