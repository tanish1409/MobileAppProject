package com.example.network.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.User

class UserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit,
    private val onRemoveFriend: (User) -> Unit,
    private val showRemoveButton: Boolean = false
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userLocation: TextView = itemView.findViewById(R.id.userLocation)
        val removeFriendBtn: Button = itemView.findViewById(R.id.removeFriendBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_list_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        // Set name & location
        holder.userName.text = user.name
        holder.userLocation.text = user.location ?: "Unknown Location"

        // Handle clicking the card
        holder.itemView.setOnClickListener { onUserClick(user) }

        if (showRemoveButton) {
            holder.removeFriendBtn.visibility = View.VISIBLE
            holder.removeFriendBtn.setOnClickListener {
                onRemoveFriend(user)
            }
        } else {
            holder.removeFriendBtn.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = users.size

    // Allow updating list without recreating the adapter
    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
