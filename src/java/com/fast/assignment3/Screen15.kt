package com.fast.assignment3

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class Screen15 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen15)
        val imageIds = listOf(
            R.drawable.profile1, R.drawable.profile2, R.drawable.profile3, R.drawable.profile4,
            R.drawable.profile5, R.drawable.profile6, R.drawable.profile7, R.drawable.profile8,
            R.drawable.profile1, R.drawable.profile2, R.drawable.profile3, R.drawable.profile4,
            R.drawable.profile5, R.drawable.profile6, R.drawable.profile7, R.drawable.profile8
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewGallery)
        val imgPreview = findViewById<ImageView>(R.id.imgPreview)

        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.adapter = GalleryAdapter(imageIds) { imageId ->
            imgPreview.setImageResource(imageId) // Directly set image resource
        }

        val goToscreen17= findViewById<TextView>(R.id.tvNext)
        val goToscreen16= findViewById<ImageView>(R.id.icon_camera)
        val back= findViewById<ImageView>(R.id.crossk_button)
        goToscreen17.setOnClickListener {
            val intent = Intent(this, Screen17::class.java)
            startActivity(intent)
        }
        goToscreen16.setOnClickListener {
            val intent = Intent(this, Screen16::class.java)
            startActivity(intent)
        }
        back.setOnClickListener {
            finish()
        }


    }
}