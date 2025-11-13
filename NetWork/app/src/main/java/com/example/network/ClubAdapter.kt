package com.example.network.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.Club

class ClubAdapter(
    private val clubs: List<Club>,
    private val onClubClick: (Club) -> Unit
) : RecyclerView.Adapter<ClubAdapter.ClubViewHolder>() {

    class ClubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.clubName)
        val sport: TextView = itemView.findViewById(R.id.clubSport)
        val description: TextView = itemView.findViewById(R.id.clubDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_club, parent, false)
        return ClubViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClubViewHolder, position: Int) {
        val club = clubs[position]

        holder.name.text = club.name
        holder.sport.text = club.sportType
        holder.description.text = club.description ?: "No description"

        holder.itemView.setOnClickListener {
            onClubClick(club)
        }
    }

    override fun getItemCount(): Int = clubs.size
}
