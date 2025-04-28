package com.fast.assignment3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StoryAdapter(
    private val stories: List<Story>,
    private var currentUser: User?,
    private val onStoryClick: () -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    fun updateCurrentUser(user: User?) {
        this.currentUser = user
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        if (position == 0) {
            holder.bindCurrentUser(currentUser, onStoryClick)
        } else {
            holder.bind(stories[position - 1])
        }
    }

    override fun getItemCount(): Int {
        return stories.size + 1
    }

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.story_image_inner)
        private val addIcon: ImageView = view.findViewById(R.id.add_icon)

        fun bindCurrentUser(user: User?, onClick: () -> Unit) {
            val profileImageUrl = user?.profileImageUrl ?: "https://via.placeholder.com/150"
            Glide.with(imageView.context).load(profileImageUrl).into(imageView)
            addIcon.visibility = View.VISIBLE
            itemView.setOnClickListener { onClick() }
        }

        fun bind(story: Story) {
            val bitmap = Utils.decodeBase64ToBitmap(story.imageBase64)
            imageView.setImageBitmap(bitmap)
            addIcon.visibility = View.GONE
        }
    }
}