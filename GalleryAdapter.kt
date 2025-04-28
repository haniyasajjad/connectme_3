package com.fast.assignment3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class GalleryAdapter(
    private val images: List<Int>,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgGallery: ImageView = itemView.findViewById(R.id.imgGallery)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val imageRes = images[position]
        holder.imgGallery.setImageResource(imageRes)

        holder.itemView.setOnClickListener {
            onImageClick(imageRes)
        }
    }

    override fun getItemCount(): Int = images.size
}
