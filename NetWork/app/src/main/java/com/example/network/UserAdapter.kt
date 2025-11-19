package com.example.network.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.User // Assuming your User model is here

class UserAdapter(
    private var users: List<User>,
    private val clickListener: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.memberNameText)
        val locationText: TextView = view.findViewById(R.id.memberLocationText)
        // You would typically bind the ImageView here as well
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_list_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nameText.text = user.name
        holder.locationText.text = user.location ?: "Unknown Location"

        holder.itemView.setOnClickListener {
            clickListener(user)
        }

        // Future idea for owner: Show the "Remove" button if the current user is the owner
        // val removeBtn = holder.itemView.findViewById<Button>(R.id.memberActionButton)
        // if (isOwner) { removeBtn.visibility = View.VISIBLE }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}