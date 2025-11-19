package com.example.network.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.Event
import com.example.network.model.User

class EventAdapter(
    private var events: List<Event>,
    private val userId: Int,
    private val joinLeaveListener: (Event) -> Unit,
    private val isUserAttendingChecker: (Int, Int) -> Boolean,
    private val onItemClickListener: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.eventTitleText)
        val dateTimeText: TextView = view.findViewById(R.id.eventDateTimeText)
        val participantsText: TextView = view.findViewById(R.id.eventParticipantsText)
        val actionBtn: Button = view.findViewById(R.id.joinLeaveEventBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_list_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Format date and time (using your existing format)
        val dateTimeString = "${event.date} at ${event.time}"

        holder.titleText.text = event.title
        holder.dateTimeText.text = dateTimeString
        holder.participantsText.text = "${event.currentParticipants}/${event.maxParticipants} participants"

        // --- JOIN/LEAVE LOGIC ---
        // We use the checker function passed from the activity
        val isAttending = isUserAttendingChecker(event.eventId, userId)

        if (event.hostId == userId) {
            // User is the host (owner of the event)
            holder.actionBtn.text = "Host"
            holder.actionBtn.isEnabled = false
            holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        } else if (isAttending) {
            // User is already attending
            holder.actionBtn.text = "Leave"
            holder.actionBtn.isEnabled = true
            holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
        } else if (event.currentParticipants >= event.maxParticipants) {
            // Event is full
            holder.actionBtn.text = "Full"
            holder.actionBtn.isEnabled = false
            holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        } else {
            // User can join
            holder.actionBtn.text = "Join"
            holder.actionBtn.isEnabled = true
            holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark))
        }

        holder.actionBtn.setOnClickListener {
            // Only fire the listener if the user is not the host
            if (event.hostId != userId) {
                joinLeaveListener(event)
            }
        }
        holder.itemView.setOnClickListener {
            onItemClickListener(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}