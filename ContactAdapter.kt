package com.fast.assignment3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private val contactList: List<Contact>,
    private val onActionClick: (Contact, String) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val name: TextView = itemView.findViewById(R.id.name)
        val acceptButton: Button = itemView.findViewById(R.id.accept_button)
        val rejectButton: Button = itemView.findViewById(R.id.reject_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_new, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.name.text = contact.name
        holder.acceptButton.visibility = View.VISIBLE
        holder.rejectButton.visibility = View.VISIBLE

        holder.acceptButton.setOnClickListener { onActionClick(contact, "accept") }
        holder.rejectButton.setOnClickListener { onActionClick(contact, "reject") }
    }

    override fun getItemCount(): Int = contactList.size
}
