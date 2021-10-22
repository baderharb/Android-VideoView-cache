package crocodile8008.videoviewcache.lib

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.VideoView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class VideoViewCached : FrameLayout {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    lateinit var videoView: VideoView
        private set

    lateinit var progressBar: ProgressBar
        private set

    var mediaPlayer: MediaPlayer? = null
        private set

    var autoScale = true

    var mpPreparedListener: MediaPlayer.OnPreparedListener? = null
    var mpErrorListener: MediaPlayer.OnErrorListener? = null
    var commonErrorListener: (t: Throwable) -> Unit = {}

    private var playCalled = false
    private var videoToLoad: VideoRequestParam? = null
    private var isLoading = false
    private val disposables = CompositeDisposable()

    private fun init() {
        val tmpFrame = LayoutInflater.from(context)
            .inflate(R.layout.cached_video_view, this, false) as ViewGroup
        videoView = tmpFrame.findViewById(R.id.cached_video_view_video)
        progressBar = tmpFrame.findViewById(R.id.cached_video_view_pb)
        tmpFrame.removeAllViews()
        addView(videoView)
        addView(progressBar)

        videoView.setOnPreparedListener { mp: MediaPlayer ->
            progressBar.visibility = GONE
            mediaPlayer = mp
            if (autoScale) {
                post {
                    // At this moment it's not working:
                    // mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    // So scale video view manually
                    try {
                        val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
                        val frameRatio = width / height.toFloat()
                        val scaleXNew = videoRatio / frameRatio
                        log(
                            "scale x: $scaleXNew" +
                                    ". video: ${mp.videoWidth} / ${mp.videoHeight} ($videoRatio)" +
                                    ". frame: $width / $height ($frameRatio)"
                        )
                        if (scaleXNew >= 1f) {
                            videoView.scaleY = scaleXNew
                        } else {
                            videoView.scaleX = 1f / scaleXNew
                        }
                    } catch (t: Throwable) {
                        commonErrorListener(t)
                    }
                }
            }
            mpPreparedListener?.onPrepared(mp)
        }

        videoView.setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
            progressBar.visibility = GONE
            mpErrorListener?.onError(mp, what, extra)
            true
        }
    }

    @Suppress("Unused")
    fun playUrl(url: String, headers: Map<String, String>? = null) {
        disposables.clear()
        progressBar.visibility = VISIBLE
        videoToLoad = VideoRequestParam(url, headers)
        loadVideoIfHasToLoad()
        playCalled = true
    }

    @Suppress("Unused")
    fun stop() {
        disposables.clear()
        videoView.stopPlayback()
        playCalled = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadVideoIfHasToLoad()
        if (playCalled) {
            videoView.start()
        }
    }

    override fun onDetachedFromWindow() {
        disposables.clear()
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && playCalled && !videoView.isPlaying) {
            videoView.start()
        }
    }

    private fun loadVideoIfHasToLoad() {
        if (isLoading) {
            return
        }
        videoToLoad?.let {
            isLoading = true
            progressBar.visibility = VISIBLE
            val disposable = VideoViewCache
                .loadInFileCached(it.url, it.headers, context)
                .doFinally {
                    isLoading = false
                }
                .subscribe(
                    { filePath ->
                        if (filePath.isEmpty()) {
                            return@subscribe
                        }
                        videoView.setVideoPath(filePath)
                        videoView.start()
                        videoToLoad = null
                    },
                    { t ->
                        progressBar.visibility = GONE
                        log("$t")
                        commonErrorListener(t)
                    }
                )
            disposables.add(disposable)
        }
    }
}

private data class VideoRequestParam(
    val url: String,
    val headers: Map<String, String>?,
)