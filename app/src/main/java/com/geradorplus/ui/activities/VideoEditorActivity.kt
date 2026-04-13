package com.geradorplus.ui.activities

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
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
import androidx.core.content.FileProvider
import com.geradorplus.databinding.ActivityVideoEditorBinding
import com.geradorplus.ui.viewmodels.BannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.sin

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
        supportActionBar?.title = "Criar Video Banner"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        binding.btnGenerateVideo.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Preencha o titulo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startVideoGeneration()
        }
    }

    private fun startVideoGeneration() {
        val user = viewModel.currentUser ?: return
        binding.progressVideo.visibility = View.VISIBLE
        binding.btnGenerateVideo.isEnabled = false
        binding.tvStatus.text = "Gerando video..."

        scope.launch {
            val outputPath = withContext(Dispatchers.IO) {
                generateVideoInternal(
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
                binding.tvStatus.text = "Video gerado com sucesso!"
                binding.cardResult.visibility = View.VISIBLE
                setupShareButton(outputPath)
            } else {
                binding.tvStatus.text = "Erro ao gerar video"
            }
        }
    }

    private fun generateVideoInternal(
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
            val surface: Surface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIndex in 0 until TOTAL_FRAMES) {
                val progress = frameIndex.toFloat() / TOTAL_FRAMES
                drawFrame(surface, frameIndex, progress, title, subtitle, promoText, serverName, contact, accentColor)

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
                                bufferInfo.presentationTimeUs = frameIndex * 1_000_000L / FRAME_RATE
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outputBufferId, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
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

    private fun drawFrame(
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
        val canvas: Canvas = surface.lockCanvas(null)
        try {
            val w = VIDEO_WIDTH.toFloat()
            val h = VIDEO_HEIGHT.toFloat()

            canvas.drawColor(Color.parseColor("#0D0D0D"))

            val grad = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(Color.parseColor("#0D0D0D"), Color.parseColor("#1A0D2E"), Color.parseColor("#0D0D1A")),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = grad })

            // Estrelas
            val starPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            val rng = Random(42)
            repeat(40) { i ->
                val sx = rng.nextFloat() * w
                val sy = (rng.nextFloat() * h + frameIndex * 2f) % h
                starPaint.alpha = ((sin(frameIndex * 0.1 + i) + 1) / 2 * 200).toInt()
                canvas.drawCircle(sx, sy, 2f + rng.nextFloat() * 3f, starPaint)
            }

            // Linha accent
            val linePaint = Paint().apply { color = accentColor; strokeWidth = 6f; isAntiAlias = true }
            val lineProgress = (progress * 1.5f).coerceIn(0f, 1f)
            canvas.drawLine(80f, h * 0.55f, 80f + (w - 160f) * lineProgress, h * 0.55f, linePaint)

            // Titulo
            val titleX = if (progress < 0.3f) -w + (w + 80f) * (progress / 0.3f) else 80f
            canvas.drawText(title, titleX, h * 0.60f, Paint().apply {
                color = Color.WHITE; textSize = 80f; isFakeBoldText = true; isAntiAlias = true
            })

            // Subtitulo
            if (subtitle.isNotBlank()) {
                val subAlpha = if (progress > 0.4f) ((progress - 0.4f) / 0.2f * 255).toInt().coerceIn(0, 255) else 0
                canvas.drawText(subtitle, 80f, h * 0.67f, Paint().apply {
                    color = Color.parseColor("#AAAAAA"); textSize = 44f; isAntiAlias = true; alpha = subAlpha
                })
            }

            // Promo
            if (promoText.isNotBlank()) {
                val pulse = (sin(frameIndex * 0.15) * 0.1 + 1.0).toFloat()
                canvas.drawText(promoText, w / 2f, h * 0.78f, Paint().apply {
                    color = accentColor; textSize = 72f * pulse; isFakeBoldText = true
                    isAntiAlias = true; textAlign = Paint.Align.CENTER
                })
            }

            // Bottom bar
            canvas.drawRect(0f, h - 200f, w, h, Paint().apply { color = Color.parseColor("#1A1A2E") })
            canvas.drawLine(0f, h - 200f, w, h - 200f, linePaint)

            if (serverName.isNotBlank()) {
                canvas.drawText(serverName, 60f, h - 120f, Paint().apply {
                    color = Color.WHITE; textSize = 40f; isFakeBoldText = true; isAntiAlias = true
                })
            }
            if (contact.isNotBlank()) {
                canvas.drawText(contact, 60f, h - 70f, Paint().apply {
                    color = Color.parseColor("#AAAAAA"); textSize = 34f; isAntiAlias = true
                })
            }
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun setupShareButton(filePath: String) {
        binding.btnShareVideo.setOnClickListener {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Compartilhar Video"
            ))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
