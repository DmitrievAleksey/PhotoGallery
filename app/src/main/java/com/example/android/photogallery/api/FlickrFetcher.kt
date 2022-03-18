package com.example.android.photogallery.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.photogallery.GalleryItem
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FlickrFetcher"

/* класс репозитория, определяющего доступ к данным с веб-сервера Flickr */

class FlickrFetcher {

    private val flickrApi: FlickrApi
    /* объявление переменной запроса */
    private lateinit var flickrRequest: Call<FlickrResponse>

    init {
        /* объявление и инициализация переменной для хранения экземпляра интерфейса Retrofit */
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://api.flickr.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        /* инициализация интефейса API значением экземпляра интерфейса FlickrApi,
        * созданного экземпляром Retrofit */
        flickrApi = retrofit.create(FlickrApi::class.java)
    }

    fun fetchPhotos(): LiveData<List<GalleryItem>> {
        /* объявление переменной для сохранения возвращаемого объекта LiveData */
        val responseLiveData: MutableLiveData<List<GalleryItem>> = MutableLiveData()
        /* инициализация веб-запроса */
        flickrRequest = flickrApi.fetchContents()

        /* вызов функции выполнения веб-запроса enqueue, находящегося во flickrRequest */
        flickrRequest.enqueue(object : Callback<FlickrResponse> {

            /* если нет ответа на запрос */
            override fun onFailure(call: Call<FlickrResponse>, t: Throwable) {
                Log.e(TAG, "Не удалось получить фотографии", t)
            }

            /* если ответ от сервера получен */
            override fun onResponse(call: Call<FlickrResponse>,
                                    response: Response<FlickrResponse>) {
                Log.d(TAG, "Полученный ответ")
                val flickrResponse: FlickrResponse? = response.body()
                val photoResponse: PhotoResponse? = flickrResponse?.photos
                var galleryItems: List<GalleryItem> = photoResponse?.galleryItems
                    ?: mutableListOf()
                galleryItems = galleryItems.filterNot { it.url.isBlank() }

                responseLiveData.value = galleryItems
            }
        })

        return responseLiveData
    }

    /* Аннотация указывает, что функция должна вызываться только в фоновом потоке */
    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response: Response<ResponseBody> = flickrApi.fetchUrlBytes(url).execute()
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        Log.i(TAG, "Декодированный bitmap=$bitmap из Response=$response")
        return bitmap
    }

    /* отмена запроса */
    fun cancelRequest() {
        if (::flickrRequest.isInitialized) {
            flickrRequest.cancel()
        }
    }

}