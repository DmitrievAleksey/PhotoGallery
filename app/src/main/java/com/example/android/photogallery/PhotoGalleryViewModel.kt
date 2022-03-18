package com.example.android.photogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.photogallery.api.FlickrFetcher

class PhotoGalleryViewModel : ViewModel() {

    private var flickrFetcher: FlickrFetcher = FlickrFetcher()
    val galleryItemLiveData: LiveData<List<GalleryItem>> = flickrFetcher.fetchPhotos()

    override fun onCleared() {
        super.onCleared()
        flickrFetcher.cancelRequest()
    }
}