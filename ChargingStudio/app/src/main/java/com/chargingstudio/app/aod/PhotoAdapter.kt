package com.chargingstudio.app.aod

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.chargingstudio.app.R

class PhotoAdapter(private val uris: MutableList<String>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoHolder>() {

    class PhotoHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false) as ImageView
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        holder.image.setImageURI(Uri.parse(uris[position]))
    }

    override fun getItemCount(): Int = uris.size
}
