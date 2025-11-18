package com.example.network

import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater// --- FIX: The incorrect 'LayoutInflaterimport' and duplicate 'View' imports have been removed ---
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.network.model.Review

class ReviewAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.reviewerName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRating)
        val reviewText: TextView = itemView.findViewById(R.id.reviewComment)
        val mediaImage: ImageView = itemView.findViewById(R.id.reviewMedia)
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
        if (review.mediaUrl.isNullOrEmpty()) {
            holder.mediaImage.visibility = View.GONE
        } else {
            holder.mediaImage.visibility = View.VISIBLE
            val uri = Uri.parse(review.mediaUrl)

            if (review.mediaUrl.endsWith(".mp4")) {
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    review.mediaUrl,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                if (thumbnail != null) {
                    holder.mediaImage.setImageBitmap(thumbnail)
                } else {
                    // Optional: Set a placeholder if thumbnail creation fails
                    // holder.mediaImage.setImageResource(R.drawable.ic_video_placeholder)
                }
            } else {
                try {
                    holder.mediaImage.setImageURI(uri)
                } catch (e: Exception) {
                    // Optional: Set a placeholder if the image URI is invalid
                    // holder.mediaImage.setImageResource(R.drawable.ic_image_placeholder)
                }
            }
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateData(newList: List<Review>) {
        reviews = newList
        notifyDataSetChanged()
    }
}
