package com.example.android.photogallery.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

/* каждая функция  в интерфейсе привязывается к http-запросу аннотацией метода http-запроса */

interface FlickrApi {

    @GET("services/rest/?method=flickr.interestingness.getList" +
            "&api_key=62bf7f21f9911de0db1a6931f2aa94f4" +
            "&format=json&nojsoncallback=1" +
            "&extras=url_s"
    )
    fun fetchContents(): Call<FlickrResponse>

    @GET
    fun fetchUrlBytes(@Url url: String): Call<ResponseBody>
}