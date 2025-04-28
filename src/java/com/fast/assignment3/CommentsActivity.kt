package com.fast.assignment3

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase

class CommentsActivity : AppCompatActivity() {
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var postId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_comments)

        postId = intent.getStringExtra("postId") ?: ""

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_comments)
        recyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(commentList)
        recyclerView.adapter = commentAdapter

        fetchComments()

        val sendButton = findViewById<ImageView>(R.id.send_comment)
        val commentInput = findViewById<EditText>(R.id.input_comment)

        sendButton.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text)
                commentInput.text.clear()
            }
        }
    }

    private fun fetchComments() {
        val commentsRef = database.getReference("comments").child(postId)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null) {
                        commentList.add(comment)
                    }
                }
                commentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun postComment(text: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val commentsRef = database.getReference("comments").child(postId)

        val commentId = commentsRef.push().key ?: return
        val comment = Comment(commentId, userId, "Username", "ProfileURL", text)

        commentsRef.child(commentId).setValue(comment)
    }
}
