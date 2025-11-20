package com.example.network

import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.model.Review
import android.media.MediaPlayer
import android.widget.Button
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.File

class ReviewAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUrl: String? = null
    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.reviewerName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRating)
        val reviewText: TextView = itemView.findViewById(R.id.reviewComment)
        val mediaImage: ImageView = itemView.findViewById(R.id.reviewMedia)
        val audioContainer: ConstraintLayout = itemView.findViewById(R.id.audioContainer)
        val audioPlayPauseIcon: ImageView = itemView.findViewById(R.id.audioPlayPauseIcon)
        val audioLabel: TextView = itemView.findViewById(R.id.audioLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        // This uses the correct property names from your Review model
        holder.userName.text = review.userName
        holder.ratingBar.rating = review.rating.toFloat()

        if (review.text.isNullOrBlank()) {
            holder.reviewText.visibility = View.GONE
        } else {
            holder.reviewText.visibility = View.VISIBLE
            holder.reviewText.text = review.text
        }

        // Media (image or video)
        // Inside ReviewAdapter.kt, replacing the media section of onBindViewHolder

        // Determine if the URL points to a Voice Note (e.g., .mp3 or .m4a)
        val isVoiceNote = review.mediaUrl.isNullOrEmpty().not() &&
                (review.mediaUrl!!.endsWith(
                    ".mp3",
                    ignoreCase = true
                ) || review.mediaUrl!!.endsWith(".m4a", ignoreCase = true))

        // Determine if this item is currently the one playing audio
        val isCurrentItemPlaying =
            review.mediaUrl == currentlyPlayingUrl && mediaPlayer?.isPlaying == true


        if (review.mediaUrl.isNullOrEmpty()) {
            // If no media is present, hide all media controls
            holder.mediaImage.visibility = View.GONE
            holder.audioContainer.visibility = View.GONE
            holder.reviewText.visibility = View.VISIBLE // Ensure comment is visible if no media
            return
        }

        if (isVoiceNote) {
            // --- VOICE NOTE LOGIC ---
            holder.mediaImage.visibility = View.GONE
            holder.audioContainer.visibility = View.VISIBLE
            // OPTIONAL: Hide comment if voice note is attached, uncomment if desired:
            // holder.reviewText.visibility = View.GONE

            // Set icon based on playback state
            holder.audioPlayPauseIcon.setImageResource(
                if (isCurrentItemPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            )

            // Set the click listener for play/pause toggle
            holder.audioPlayPauseIcon.setOnClickListener {
                if (isCurrentItemPlaying) {
                    // Action: Pause
                    mediaPlayer?.pause()
                    currentlyPlayingUrl = null
                } else if (mediaPlayer?.isPlaying == true) {
                    // Action: Stop current audio and start new one
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    currentlyPlayingUrl = null
                    notifyDataSetChanged() // Refresh old item's icon to 'Play'

                    playAudio(review.mediaUrl!!, holder.audioPlayPauseIcon)
                } else {
                    // Action: Play (or Resume if paused and same URL)
                    if (review.mediaUrl == currentlyPlayingUrl) {
                        // Resume if same URL
                        mediaPlayer?.start()
                    } else {
                        // Play new audio
                        playAudio(review.mediaUrl!!, holder.audioPlayPauseIcon)
                    }
                }
                notifyDataSetChanged() // Refresh view to update icon
            }
        } else {
            // --- IMAGE/VIDEO LOGIC ---
            holder.audioContainer.visibility = View.GONE
            holder.reviewText.visibility = View.VISIBLE // Ensure comment is visible

            holder.mediaImage.visibility = View.VISIBLE
            val uri = Uri.parse(review.mediaUrl)

            if (review.mediaUrl.endsWith(".mp4", ignoreCase = true)) {
                // Existing Video Thumbnail Code
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    review.mediaUrl,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                if (thumbnail != null) {
                    holder.mediaImage.setImageBitmap(thumbnail)
                } else {
                    // Use a placeholder if thumbnail fails
                    // holder.mediaImage.setImageResource(R.drawable.ic_video_placeholder)
                }
            } else {
                // Existing Image Loading Code
                try {
                    holder.mediaImage.setImageURI(uri)
                } catch (e: Exception) {
                    // Handle invalid image URI
                }
            }
        }
    }

    private fun playAudio(path: String, iconView: ImageView) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(iconView.context, "Audio file not found.", Toast.LENGTH_SHORT).show()
                return
            }

            mediaPlayer = MediaPlayer().apply {
                val uri = Uri.fromFile(file)
                setDataSource(iconView.context, uri)
                setOnPreparedListener { mp ->
                    mp.start()
                    currentlyPlayingUrl = path
                    iconView.setImageResource(R.drawable.ic_pause)
                    notifyDataSetChanged()
                }
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                    currentlyPlayingUrl = null
                    notifyDataSetChanged()
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Toast.makeText(iconView.context, "Playback error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingUrl = null
    }

    override fun getItemCount(): Int = reviews.size

    fun updateData(newList: List<Review>) {
        reviews = newList
        notifyDataSetChanged()
    }
}
