package com.example.android.photogallery

import com.google.gson.annotations.SerializedName

/* объект модели для сопостовления с json-данными, полученными с запроса.
* модель сопоставляется с объектом в json-массиве photo */

data class GalleryItem(
    var title: String = "",
    var id: String = "",
    @SerializedName("url_s") var url: String = ""
)
