package com.example.network.adapters

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.network.R
import com.example.network.model.Media
import java.io.File

class MediaAdapter(
    private val mediaList: List<Media>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.mediaImage)
        val playIcon: ImageView = view.findViewById(R.id.playIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(v)
    }

    override fun getItemCount(): Int = mediaList.size

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = mediaList[position]
        val context = holder.itemView.context
        val uri = resolveUri(context, media.url)

        if (media.type == "photo") {
            holder.playIcon.visibility = View.GONE

            // SAFE LOAD (fixes Google Photos crash)
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(stream)
                holder.image.setImageBitmap(bmp)
                stream?.close()
            } catch (e: Exception) {
                holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }

        } else {
            holder.playIcon.visibility = View.VISIBLE
            holder.image.setImageResource(android.R.drawable.ic_media_play)
            holder.image.scaleType = ImageView.ScaleType.CENTER

            holder.itemView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
        }
    }

    private fun resolveUri(context: android.content.Context, url: String): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            File(url)
        )
    }
}
