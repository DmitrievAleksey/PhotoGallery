package com.example.android.photogallery

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.android.photogallery.api.FlickrFetcher
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ThumbnailDownloader"

/* Значение будет использоваться для идентификации сообщений как запросов на загрузку (
* присваивается полю what, создаваемых сообщений загрузки ) */
private const val MESSAGE_DOWNLOAD = 0

/* Класс для создания фонового потока. класс расширяет HandlerThread, передается
* обобщенный аргумент <T>, чтобы не ограничиваться конктерным типом объекта для
* идентификации каждой загрузки и определения элемента пользовательского интерфейса,
* который должен обновляться после загрузки.
* Activity и фрагмент будут являться владельцами жизненного цикла, для наблюдения
* за жизненным циклом будет использоваться компонент наблюдателя (владельца)
* LifecycleObserver */

class ThumbnailDownloader<in T>(
    /* экземпляр Handler, переданный из главного потока */
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
) : HandlerThread(TAG) {

    val fragmentLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver {
            /* Аннотация OnLifecycleEvent позволяет ассоциировать функцию с обратным вызовом
            * жизненного цикла от любого владельца LifecycleOwner. Событие Event.ON_CREATE
            * регистрирует вызов аннотируемой функции при вызове LifecycleOwner.onCreate */
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun setup() {
                Log.i(TAG, "Запуск фонового потока")
                start()
                looper
            }

            /* Событие Event.ON_DESTROY регистрирует вызов аннотируемой функции при вызове
            * функции LifecycleOwner.onDestroy */
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(TAG, "Уничтожение фонового потока")
                quit()
            }
        }

    val viewLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(TAG, "Удаление всех запросов из очереди")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }

    private var hasQuit = false
    /* ссылка на объект Handler, отвечающий за постановку в очередь запросов на загрузку
    * в созданном ФОНОВОМ потоке и обработку сообщений запросов на загрузку при извлечении
    * из очереди */
    private lateinit var requestHandler: Handler
    /* в ключе Т объекта ConcurrentHashMap будет хранится url-адрес, связанный с конкретным
    * запросом.  */
    private val requestMap = ConcurrentHashMap<T, String>()
    /* ссылка на экземпляр FlickrFetcher */
    private val flickrFetcher = FlickrFetcher()

    /* создание реализации обработчика */
    /* suppress сообщает Lint, что msg.obj приводится к типу Т без предварительной проверки на
    * соответствие */
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    /* функция вызываетсая до того, как Looper впервые проверит очередь сообщений */
    override fun onLooperPrepared() {
        requestHandler = object : Handler() {
            /* обработка сообщения */
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    Log.i(TAG, "Получить запрос на url: ${requestMap[target]}")
                    handleRequest(target)
                }
            }
        }
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    /* функция получает объект типа T, выполняющий функции идентификатора загрузки и
    * url-адрес для загрузки */
    fun queueThumbnail(target: T, url: String) {
        Log.i(TAG, "Получить url: $url")
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
            .sendToTarget()
    }

    fun clearQueue() {
        requestHandler.removeMessages(MESSAGE_DOWNLOAD)
        requestMap.clear()
    }

    private fun handleRequest(target: T) {
        val url = requestMap[target] ?: return
        val bitmap = flickrFetcher.fetchPhoto(url) ?: return

        responseHandler.post(Runnable {
            if (requestMap[target] != url || hasQuit) {
                return@Runnable
            }

            requestMap.remove(target)
            onThumbnailDownloaded(target, bitmap)
        })
    }
}