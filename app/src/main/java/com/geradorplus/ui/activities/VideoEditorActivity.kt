package com.geradorplus.ui.activities

import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.geradorplus.data.models.Banner
import com.geradorplus.databinding.ActivityVideoEditorBinding
import com.geradorplus.ui.viewmodels.BannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private val viewModel: BannerViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val VIDEO_WIDTH = 1080
        const val VIDEO_HEIGHT = 1920
        const val FRAME_RATE = 30
        const val DURATION_SECONDS = 10
        const val TOTAL_FRAMES = FRAME_RATE * DURATION_SECONDS
        const val BIT_RATE = 4_000_000
        const val MIME_TYPE = "video/avc"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Criar Vídeo Banner"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        binding.btnGenerateVideo.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Preencha o título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startVideoGeneration()
        }
    }

    private fun startVideoGeneration() {
        val user = viewModel.currentUser ?: return

        binding.progressVideo.visibility = View.VISIBLE
        binding.btnGenerateVideo.isEnabled = false
        binding.tvStatus.text = "Gerando vídeo..."

        scope.launch {
            val outputPath = withContext(Dispatchers.IO) {
                generateVideo(
                    title = binding.etTitle.text.toString(),
                    subtitle = binding.etSubtitle.text.toString(),
                    promoText = binding.etPromoText.text.toString(),
                    serverName = user.serverName ?: "",
                    contact = user.contactInfo ?: "",
                    accentColor = 0xFFE94560.toInt()
                )
            }

            binding.progressVideo.visibility = View.GONE
            binding.btnGenerateVideo.isEnabled = true

            if (outputPath != null) {
                binding.tvStatus.text = "✅ Vídeo gerado com sucesso!"
                binding.cardResult.visibility = View.VISIBLE
                setupShareButton(outputPath)
            } else {
                binding.tvStatus.text = "❌ Erro ao gerar vídeo"
            }
        }
    }

    private fun generateVideo(
        title: String,
        subtitle: String,
        promoText: String,
        serverName: String,
        contact: String,
        accentColor: Int
    ): String? {
        return try {
            val dir = File(getExternalFilesDir(null), "GeradorPlus/videos")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(dir, "video_$timestamp.mp4")

            val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()

            // Draw each frame onto the surface
            for (frameIndex in 0 until TOTAL_FRAMES) {
                val progress = frameIndex.toFloat() / TOTAL_FRAMES
                drawFrameToSurface(surface, frameIndex, progress, title, subtitle, promoText, serverName, contact, accentColor)

                // Drain encoder
                var outputDone = false
                while (!outputDone) {
                    val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> outputDone = true
                        outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputBufferId >= 0 -> {
                            val encodedData = codec.getOutputBuffer(outputBufferId)
                            if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                                bufferInfo.presentationTimeUs = (frameIndex * 1_000_000L / FRAME_RATE)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outputBufferId, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
                }

                // Update progress on main thread
                val progressPercent = ((frameIndex.toFloat() / TOTAL_FRAMES) * 100).toInt()
                withContext(Dispatchers.Main) {
                    binding.progressVideo.progress = progressPercent
                    binding.tvStatus.text = "Gerando vídeo... $progressPercent%"
                }
            }

            codec.signalEndOfInputStream()
            codec.stop()
            codec.release()
            surface.release()
            muxer.stop()
            muxer.release()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawFrameToSurface(
        surface: Surface,
        frameIndex: Int,
        progress: Float,
        title: String,
        subtitle: String,
        promoText: String,
        serverName: String,
        contact: String,
        accentColor: Int
    ) {
        val canvas = surface.lockCanvas(null)
        try {
            // Background
            canvas.drawColor(Color.parseColor("#0D0D0D"))

            val w = VIDEO_WIDTH.toFloat()
            val h = VIDEO_HEIGHT.toFloat()

            // Animated gradient background
            val animPhase = (frameIndex % 60) / 60f
            val gradColors = intArrayOf(
                Color.parseColor("#0D0D0D"),
                Color.parseColor("#1A0D2E"),
                Color.parseColor("#0D0D1A")
            )
            val grad = LinearGradient(0f, 0f, w, h, gradColors, null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = grad })

            // Animated particles / stars
            val starPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            val random = Random(42)
            repeat(40) {
                val sx = random.nextFloat() * w
                val sy = ((random.nextFloat() * h + frameIndex * 2f) % h)
                val alpha = ((Math.sin(frameIndex * 0.1 + it) + 1) / 2 * 200).toInt()
                starPaint.alpha = alpha
                canvas.drawCircle(sx, sy, 2f + random.nextFloat() * 3f, starPaint)
            }

            // Animated accent line
            val lineProgress = (progress * 1.5f).coerceIn(0f, 1f)
            val linePaint = Paint().apply {
                color = accentColor
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawLine(80f, h * 0.55f, 80f + (w - 160f) * lineProgress, h * 0.55f, linePaint)

            // Title animation — slide in from left
            val titleX = if (progress < 0.3f) {
                -w + (w + 80f) * (progress / 0.3f)
            } else 80f
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 80f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(title, titleX, h * 0.60f, titlePaint)

            // Subtitle
            if (subtitle.isNotBlank()) {
                val subAlpha = if (progress > 0.4f) ((progress - 0.4f) / 0.2f * 255).toInt().coerceIn(0, 255) else 0
                val subPaint = TextPaint().apply {
                    color = Color.parseColor("#AAAAAA")
                    textSize = 44f
                    isAntiAlias = true
                    alpha = subAlpha
                }
                canvas.drawText(subtitle, 80f, h * 0.67f, subPaint)
            }

            // Promo text — pulse animation
            if (promoText.isNotBlank()) {
                val pulse = (Math.sin(frameIndex * 0.15) * 0.1 + 1.0).toFloat()
                val promoPaint = TextPaint().apply {
                    color = accentColor
                    textSize = 72f * pulse
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                promoPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(promoText, w / 2f, h * 0.78f, promoPaint)
            }

            // Bottom bar
            canvas.drawRect(0f, h - 200f, w, h, Paint().apply { color = Color.parseColor("#1A1A2E") })
            canvas.drawLine(0f, h - 200f, w, h - 200f, linePaint)

            if (serverName.isNotBlank()) {
                val botPaint = TextPaint().apply { color = Color.WHITE; textSize = 40f; isFakeBoldText = true; isAntiAlias = true }
                canvas.drawText(serverName, 60f, h - 120f, botPaint)
            }
            if (contact.isNotBlank()) {
                val contactPaint = TextPaint().apply { color = Color.parseColor("#AAAAAA"); textSize = 34f; isAntiAlias = true }
                canvas.drawText(contact, 60f, h - 70f, contactPaint)
            }

        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun setupShareButton(filePath: String) {
        binding.btnShareVideo.setOnClickListener {
            val file = File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            startActivity(
                android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Compartilhar Vídeo"
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
