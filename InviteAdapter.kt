package com.fast.assignment3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InviteAdapter(
    private val inviteList: List<Contact>,
    private val onInviteClick: (Contact) -> Unit
) : RecyclerView.Adapter<InviteAdapter.InviteViewHolder>() {

    inner class InviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val name: TextView = itemView.findViewById(R.id.name)
        val inviteButton: Button = itemView.findViewById(R.id.invite_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite, parent, false)
        return InviteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val contact = inviteList[position]
        if (contact.profileImage.isNotEmpty()) {
            //val decodedImage = decodeBase64ToBitmap(contact.profileImage)
            //holder.profileImage.setImageBitmap(decodedImage)
            holder.profileImage.setImageResource(R.drawable.profile1)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile1) // Use a default profile image
        }

        holder.name.text = contact.name


        holder.inviteButton.text = if (contact.status == "sent") "Request Sent" else "Invite"
        holder.inviteButton.isEnabled = contact.status != "sent"

        // Send friend request on click
        holder.inviteButton.setOnClickListener {
            if (contact.status == "none") { // Only allow if not already sent
                onInviteClick(contact)
            }
        }
    }

    override fun getItemCount(): Int {
        return inviteList.size
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
