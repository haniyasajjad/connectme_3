package com.fast.assignment3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FollowAdapter(private val users: List<User>) : RecyclerView.Adapter<FollowAdapter.FollowViewHolder>() {

    inner class FollowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val speechBubble: ImageView = itemView.findViewById(R.id.speech_bubble)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_follow, parent, false)
        return FollowViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        val user = users[position]

        // Load profile image from Firebase URL using Glide
        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.profile) // Default image if URL is empty
            .into(holder.profileImage)

        // Set username
        holder.userName.text = user.userName

        // Handle speech bubble click (e.g., open chat)
        holder.speechBubble.setOnClickListener {
            // Open chat activity (implement navigation here)
        }
    }

    override fun getItemCount(): Int = users.size
}