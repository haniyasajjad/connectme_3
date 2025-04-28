package com.fast.assignment3

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PostFeedAdapter(private val postList: List<Post>) :
    RecyclerView.Adapter<PostFeedAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.post_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_feed, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        if (post.media.isNotEmpty()) {
            try {
                // Decode Base64 string to Bitmap
                val decodedBytes = Base64.decode(post.media, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Fallback to placeholder if decoding fails
                holder.imageView.setImageResource(R.drawable.sample_post)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.sample_post)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}