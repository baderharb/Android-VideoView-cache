package crocodile8008.videoviewcache.lib

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal object VideoViewCache {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun loadInFileCached(
        url: String,
        headers: Map<String, String>?,
        context: Context,
    ): Single<String> = Single
        .create { emitter: SingleEmitter<String> ->

            log("request: $url")
            var isWritingToFile = false
            var file: File? = null
            var sink: BufferedSink? = null
            var body: ResponseBody? = null

            try {
                file = getOutputFile(url, context)
                if (file.exists() && file.length() > 0) {
                    log("cached: $url")
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(file.absolutePath)
                        return@create
                    }
                }

                val request: Request = Request.Builder()
                    .url(url)
                    .also { builder ->
                        headers?.entries?.forEach { (name, value) ->
                            builder.addHeader(name, value)
                        }
                    }
                    .build()

                val response: Response = okHttpClient.newCall(request).execute()

                isWritingToFile = true
                sink = file.sink().buffer()
                body = response.body!!
                sink.writeAll(body.source())

                log("ready: $url")
                if (!emitter.isDisposed) {
                    emitter.onSuccess(file.absolutePath)
                }
            } catch (e: Exception) {
                val interrupted = Thread.interrupted()
                if (isWritingToFile && file != null) {
                    tryDeleteFile(file)
                }
                log("request exception: $e" +
                        ", was interrupted: $interrupted, isWritingToFile: $isWritingToFile")
                if (!emitter.isDisposed) {
                    emitter.tryOnError(e)
                }
            } finally {
                sink.closeSilent()
                body.closeSilent()
            }
        }
        .retry(1)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

    private fun tryDeleteFile(file: File) {
        try {
            if (file.exists()) {
                val deleted = file.delete()
                log("delete file: $deleted")
            }
        } catch (e: Error) {
            log("try delete file: $e")
        }
    }

    private fun getOutputFile(url: String, context: Context): File {
        val outputFileDir = context.cacheDir.absolutePath + "/video_view_cache/"
        val fileDir = File(outputFileDir)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        val outputFileName = abs(url.hashCode()).toString() + url.split("/").last()
        return File(outputFileDir + outputFileName)
    }
}