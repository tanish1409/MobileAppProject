package com.example.network.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.User

class FriendRequestAdapter(
    private var users: List<User>,
    private val onAccept: (User) -> Unit,
    private val onReject: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.requestUserName)
        val emailText: TextView = view.findViewById(R.id.requestUserEmail)
        val acceptBtn: Button = view.findViewById(R.id.acceptBtn)
        val rejectBtn: Button = view.findViewById(R.id.rejectBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = users[position]
        holder.nameText.text = user.name
        holder.emailText.text = user.email

        holder.acceptBtn.setOnClickListener { onAccept(user) }
        holder.rejectBtn.setOnClickListener { onReject(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
