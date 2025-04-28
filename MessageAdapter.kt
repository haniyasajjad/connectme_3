package com.fast.assignment3

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class MessageAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENDER = 1
        private const val VIEW_TYPE_RECIPIENT = 2
    }

    inner class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.sender_image_view)
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
    }

    inner class RecipientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val imageView: ImageView = itemView.findViewById(R.id.recipient_image_view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe) VIEW_TYPE_SENDER else VIEW_TYPE_RECIPIENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENDER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sender, parent, false)
            SenderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_recipient, parent, false)
            RecipientViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        Log.d("ChatDebug", "Binding message: ${message.messageId}, text: ${message.messageText}, hasImage: ${message.imageBase64 != null}, isSentByMe: ${message.isSentByMe}")

        // Format timestamp
        val timestampText = java.text.SimpleDateFormat("HH:mm").format(message.timestamp)

        if (holder is SenderViewHolder) {
            if (message.imageBase64 != null && message.imageBase64.isNotEmpty()) {
                Log.d("ChatDebug", "Sender processing image for message: ${message.messageId}, imageBase64 length: ${message.imageBase64.length}")
                holder.imageView.visibility = View.VISIBLE
                try {
                    val bitmap = Utils.decodeBase64ToBitmap(message.imageBase64)
                    holder.imageView.setImageBitmap(bitmap)
                    Log.d("ChatDebug", "Sender image set for message: ${message.messageId}")
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Error decoding sender image: ${e.message}")
                    holder.imageView.visibility = View.GONE
                }
                holder.messageText.visibility = View.GONE
            } else {
                Log.d("ChatDebug", "Sender processing text for message: ${message.messageId}")
                holder.messageText.text = message.messageText
                holder.messageText.visibility = View.VISIBLE
                holder.imageView.visibility = View.GONE
            }
            holder.timestamp.text = timestampText

            // Click listener for edit/delete (within 5 minutes)
            holder.itemView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                val timeDifference = currentTime - message.timestamp
                if (message.isSentByMe && timeDifference <= 5 * 60 * 1000) {
                    val popupMenu = PopupMenu(holder.itemView.context, holder.itemView)
                    popupMenu.menuInflater.inflate(R.menu.message_options_menu, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.edit_message -> {
                                when (holder.itemView.context) {
                                    is Screen6 -> (holder.itemView.context as Screen6).showEditDialog(message)
                                    is Screen7 -> (holder.itemView.context as Screen7).showEditDialog(message)
                                }
                                true
                            }
                            R.id.delete_message -> {
                                when (holder.itemView.context) {
                                    is Screen6 -> (holder.itemView.context as Screen6).deleteMessage(message)
                                    is Screen7 -> (holder.itemView.context as Screen7).deleteMessage(message)
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            }
        } else if (holder is RecipientViewHolder) {
            if (message.imageBase64 != null && message.imageBase64.isNotEmpty()) {
                Log.d("ChatDebug", "Recipient processing image for message: ${message.messageId}, imageBase64 length: ${message.imageBase64.length}")
                holder.imageView.visibility = View.VISIBLE
                try {
                    val bitmap = Utils.decodeBase64ToBitmap(message.imageBase64)
                    holder.imageView.setImageBitmap(bitmap)
                    Log.d("ChatDebug", "Recipient image set for message: ${message.messageId}")
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Error decoding recipient image: ${e.message}")
                    holder.imageView.visibility = View.GONE
                }
                holder.messageText.visibility = View.GONE
            } else {
                Log.d("ChatDebug", "Recipient processing text for message: ${message.messageId}")
                holder.messageText.text = message.messageText
                holder.messageText.visibility = View.VISIBLE
                holder.imageView.visibility = View.GONE
            }
            holder.timestamp.text = timestampText

            // Fetch sender's profile image
            val queue = Volley.newRequestQueue(holder.itemView.context)
            val userUrl = "http://192.168.100.40/assignment3_backend/get_user.php?user_id=${message.senderId}"
            val userRequest = JsonObjectRequest(
                Request.Method.GET,
                userUrl,
                null,
                { response ->
                    if (response.getString("status") == "success") {
                        val userJson = response.getJSONObject("user")
                        val profileImageUrl = userJson.getString("Profileimage") ?: ""
                        if (profileImageUrl.isNotEmpty()) {
                            try {
                                holder.profileImage.setImageBitmap(Utils.decodeBase64ToBitmap(profileImageUrl))
                            } catch (e: Exception) {
                                Log.e("ChatDebug", "Error decoding profile image: ${e.message}")
                                holder.profileImage.setImageResource(R.drawable.profile1)
                            }
                        } else {
                            holder.profileImage.setImageResource(R.drawable.profile1)
                        }
                    } else {
                        holder.profileImage.setImageResource(R.drawable.profile1)
                    }
                },
                { error ->
                    Log.e("ChatDebug", "Error fetching profile image: ${error.message}")
                    holder.profileImage.setImageResource(R.drawable.profile1)
                }
            )
            queue.add(userRequest)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        Log.d("ChatDebug", "Updating messages with size: ${newMessages.size}")
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
        Log.d("ChatDebug", "Adapter messages size after update: ${messages.size}")
    }
}