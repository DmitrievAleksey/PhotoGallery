package com.example.android.photogallery.api

import com.example.android.photogallery.GalleryItem
import com.google.gson.annotations.SerializedName

/* класс для сопоставляется с json-массивом photo */

class PhotoResponse {
    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}