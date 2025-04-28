package com.fast.assignment3

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.util.UUID

class PostAdapter(private val context: Context, private var posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val queue = Volley.newRequestQueue(context)

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.profilepic)
        val userName: TextView = itemView.findViewById(R.id.username_post)
        val postImage: ImageView = itemView.findViewById(R.id.postimage)
        val postCaption: TextView = itemView.findViewById(R.id.postcaption)
        val commentIcon: ImageView = itemView.findViewById(R.id.commenticon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Load Post Image
        val bitmap = Utils.decodeBase64ToBitmap(post.media)
        holder.postImage.setImageBitmap(bitmap)

        // Set Caption Initially
        holder.postCaption.text = post.caption

        // Fetch User Details from get_user.php
        val userUrl = "http://192.168.100.40/assignment3_backend/get_user.php?user_id=${post.userId}"
        val userRequest = JsonObjectRequest(
            Request.Method.GET,
            userUrl,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    val userJson = response.getJSONObject("user")
                    val userName = userJson.getString("Name") ?: "Unknown"
                    val profilePicUrl = userJson.getString("Profileimage") ?: ""

                    holder.userName.text = userName
                    holder.postCaption.text = "$userName: ${post.caption}"

                    Glide.with(context)
                        .load(profilePicUrl)
                        .placeholder(R.drawable.profile1)
                        .error(R.drawable.profile2)
                        .into(holder.profilePic)
                } else {
                    Log.e("Volley", "Error fetching user: ${response.getString("message")}")
                    holder.userName.text = "Unknown"
                }
            },
            { error ->
                Log.e("Volley", "Error fetching user: ${error.message}")
                holder.userName.text = "Unknown"
            }
        )
        queue.add(userRequest)

        // Comments Click Listener
        holder.commentIcon.setOnClickListener {
            showCommentsDialog(post.postId)
        }
    }

    private fun showCommentsDialog(postId: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_comments)
        val inputField = dialogView.findViewById<TextView>(R.id.input_comment)
        val sendButton = dialogView.findViewById<ImageView>(R.id.send_comment)

        val commentList = mutableListOf<Comment>()
        val adapter = CommentAdapter(commentList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Fetch Comments from get_comments.php
        val commentsUrl = "http://192.168.100.40/assignment3_backend/get_comments.php?post_id=$postId"
        val commentsRequest = JsonObjectRequest(
            Request.Method.GET,
            commentsUrl,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    commentList.clear()
                    val commentsArray = response.getJSONArray("comments")
                    for (i in 0 until commentsArray.length()) {
                        val commentJson = commentsArray.getJSONObject(i)
                        commentList.add(
                            Comment(
                                commentId = commentJson.getString("comment_id"),
                                postId = commentJson.getString("post_id"),
                                userId = commentJson.getString("user_id"),
                                text = commentJson.getString("text"),
                                timestamp = commentJson.getLong("timestamp")
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Log.e("Volley", "Error fetching comments: ${response.getString("message")}")
                }
            },
            { error -> Log.e("Volley", "Error fetching comments: ${error.message}") }
        )
        queue.add(commentsRequest)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Comments")
            .setPositiveButton("Close", null)
            .create()

        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                val userId = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_id", "") ?: return@setOnClickListener
                val commentId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis().toString()

                val jsonBody = JSONObject().apply {
                    put("comment_id", commentId)
                    put("post_id", postId)
                    put("user_id", userId)
                    put("text", text)
                    put("timestamp", timestamp)
                }

                // Add Comment via add_comment.php
                val commentRequest = JsonObjectRequest(
                    Request.Method.POST,
                    "http://192.168.100.40/assignment3_backend/add_comment.php",
                    jsonBody,
                    { response ->
                        if (response.getString("status") == "success") {
                            commentList.add(Comment(commentId, postId, userId, text, timestamp))
                            adapter.notifyDataSetChanged()
                            inputField.text = ""
                        } else {
                            Log.e("Volley", "Error adding comment: ${response.getString("message")}")
                        }
                    },
                    { error -> Log.e("Volley", "Error adding comment: ${error.message}") }
                )
                queue.add(commentRequest)
            }
        }

        dialog.show()
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}