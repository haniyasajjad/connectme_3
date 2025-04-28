package com.fast.assignment3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentSearchAdapter(
    private val searchList: List<Pair<Int, String>>, // Pair(userId, username)
    private val onRemove: (Pair<Int, String>) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val crossButton: ImageView = view.findViewById(R.id.cross_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (userId, username) = searchList[position]
        holder.name.text = username
        holder.crossButton.setOnClickListener { onRemove(Pair(userId, username)) }
        holder.itemView.setOnClickListener { onItemClick(userId) }
    }

    override fun getItemCount(): Int = searchList.size
}