package com.example.puretube

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.puretube.api.InvidiousApi
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var etSearchBox: EditText
    private lateinit var btnPlay: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVideoTitle: TextView

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val invidiousApi = InvidiousApi.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        etSearchBox = findViewById(R.id.etSearchBox)
        btnPlay = findViewById(R.id.btnPlay)
        progressBar = findViewById(R.id.progressBar)
        tvVideoTitle = findViewById(R.id.tvVideoTitle)

        btnPlay.setOnClickListener {
            val videoId = etSearchBox.text.toString().trim()
            if (videoId.isNotEmpty()) {
                fetchAndPlayVideo(videoId)
            } else {
                Toast.makeText(this, "Vui lòng nhập Video ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            playerView.player = mediaController
            
            // Set up a listener for picture-in-picture mode changes if needed
            // (Android automatically handles simple PiP on modern versions when user leaves app)
            
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    private fun fetchAndPlayVideo(videoId: String) {
        progressBar.visibility = View.VISIBLE
        btnPlay.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lấy thông tin video từ Invidious API
                val videoDetails = invidiousApi.getVideoDetails(videoId)
                
                withContext(Dispatchers.Main) {
                    tvVideoTitle.text = videoDetails.title

                    // Lấy stream tốt nhất (có dạng mp4)
                    val bestStream = videoDetails.formatStreams.find { 
                        it.type.contains("video/mp4") 
                    } ?: videoDetails.formatStreams.firstOrNull()

                    if (bestStream != null) {
                        playVideoUrl(bestStream.url)
                    } else {
                        Toast.makeText(this@MainActivity, "Không tìm thấy luồng phát phù hợp", Toast.LENGTH_LONG).show()
                    }
                    
                    progressBar.visibility = View.GONE
                    btnPlay.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    btnPlay.isEnabled = true
                }
            }
        }
    }

    private fun playVideoUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        mediaController?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Cho phép app thu nhỏ xuống thành cửa sổ nổi (PiP) khi bấm Home (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val player = mediaController
            if (player != null && player.isPlaying) {
                enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
            }
        }
    }
}
