package com.example.network

import android.content.Context
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

class ReviewAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.reviewUserName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRating)
        val reviewText: TextView = itemView.findViewById(R.id.reviewText)
        val mediaImage: ImageView = itemView.findViewById(R.id.reviewMedia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        // Username
        holder.userName.text = review.userName ?: "Anonymous"

        // Rating
        holder.ratingBar.rating = review.rating.toFloat()

        // Text
        holder.reviewText.text = review.text ?: ""

        // Media (image or video)
        if (review.mediaUrl.isNullOrEmpty()) {
            holder.mediaImage.visibility = View.GONE
        } else {
            holder.mediaImage.visibility = View.VISIBLE
            val context = holder.itemView.context
            val uri = Uri.parse(review.mediaUrl)

            if (review.mediaUrl.endsWith(".mp4")) {
                // Generate video thumbnail
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    review.mediaUrl,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                if (thumbnail != null) holder.mediaImage.setImageBitmap(thumbnail)
                else holder.mediaImage.setImageResource(R.drawable.ic_video_placeholder)
            } else {
                // Image
                try {
                    holder.mediaImage.setImageURI(uri)
                } catch (e: Exception) {
                    holder.mediaImage.setImageResource(R.drawable.ic_image_placeholder)
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
