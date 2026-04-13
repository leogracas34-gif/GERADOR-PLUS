package com.geradorplus.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.geradorplus.data.models.Banner
import com.geradorplus.data.models.BannerTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val BANNER_WIDTH = 1080
        const val BANNER_HEIGHT = 1920 // 9:16 Stories format
        const val BANNER_WIDTH_WIDE = 1920 // 16:9 horizontal
        const val BANNER_HEIGHT_WIDE = 1080
    }

    suspend fun generateBanner(banner: Banner, isWide: Boolean = false): String? {
        return withContext(Dispatchers.IO) {
            try {
                val width = if (isWide) BANNER_WIDTH_WIDE else BANNER_WIDTH
                val height = if (isWide) BANNER_HEIGHT_WIDE else BANNER_HEIGHT

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Draw based on template
                when (banner.template) {
                    BannerTemplate.DARK_CINEMA -> drawDarkCinema(canvas, banner, width, height)
                    BannerTemplate.NEON_GLOW -> drawNeonGlow(canvas, banner, width, height)
                    BannerTemplate.MINIMAL_ELEGANT -> drawMinimalElegant(canvas, banner, width, height)
                    BannerTemplate.EXPLOSIVE_ACTION -> drawExplosiveAction(canvas, banner, width, height)
                    BannerTemplate.SERIES_BINGE -> drawSeriesBinge(canvas, banner, width, height)
                    BannerTemplate.PROMOTION_SALE -> drawPromotionSale(canvas, banner, width, height)
                }

                saveBitmap(bitmap, banner.id)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun drawDarkCinema(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        // Background
        val bgPaint = Paint()
        bgPaint.color = Color.parseColor("#0D0D0D")
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Poster image (top 60%)
        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val destRect = RectF(0f, 0f, w.toFloat(), h * 0.65f)
                drawScaledBitmap(canvas, bmp, destRect)

                // Gradient overlay
                val gradient = LinearGradient(
                    0f, h * 0.3f, 0f, h * 0.65f,
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#0D0D0D")),
                    null, Shader.TileMode.CLAMP
                )
                val gradPaint = Paint()
                gradPaint.shader = gradient
                canvas.drawRect(0f, h * 0.3f, w.toFloat(), h * 0.65f, gradPaint)
            }
        }

        // Red accent line
        val accentPaint = Paint().apply {
            color = banner.accentColor
            strokeWidth = 6f
        }
        canvas.drawLine(80f, h * 0.67f, 300f, h * 0.67f, accentPaint)

        // Title
        drawText(canvas, banner.title, 80f, h * 0.70f, 72f, banner.textColor, Paint.Align.LEFT, true, w - 160)

        // Subtitle / Genre / Year
        val meta = listOfNotNull(banner.genre, banner.year).joinToString("  •  ")
        if (meta.isNotEmpty()) {
            drawText(canvas, meta, 80f, h * 0.76f, 36f, 0xFFAAAAAA.toInt(), Paint.Align.LEFT)
        }

        // Description
        banner.description?.let {
            drawMultilineText(canvas, it, 80f, h * 0.80f, 32f, 0xFFCCCCCC.toInt(), w - 160)
        }

        // Promotion text
        banner.promotionText?.let {
            drawPromotionBadge(canvas, it, w - 80f, h * 0.68f, banner.accentColor)
        }

        // Rating stars
        banner.rating?.let {
            drawRating(canvas, it, 80f, h * 0.87f, banner.accentColor)
        }

        // Bottom bar with logo and contact
        drawBottomBar(canvas, banner, w, h, 0xFF1A1A1A.toInt())
    }

    private fun drawNeonGlow(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        // Dark background
        canvas.drawColor(Color.parseColor("#050510"))

        // Neon gradient background
        val radialGrad = RadialGradient(
            w * 0.5f, h * 0.3f, h * 0.5f,
            intArrayOf(Color.parseColor("#1a0040"), Color.parseColor("#050510")),
            null, Shader.TileMode.CLAMP
        )
        val gradPaint = Paint().apply { shader = radialGrad }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

        // Poster with neon border
        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val margin = 60f
                val destRect = RectF(margin, h * 0.05f, w - margin, h * 0.55f)
                // Neon glow border
                val borderPaint = Paint().apply {
                    color = banner.accentColor
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.OUTER)
                }
                canvas.drawRect(destRect, borderPaint)
                drawScaledBitmap(canvas, bmp, destRect)
            }
        }

        // Title with neon glow
        val titlePaint = TextPaint().apply {
            color = banner.accentColor
            textSize = 80f
            isFakeBoldText = true
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.SOLID)
        }
        canvas.drawText(banner.title, w / 2f, h * 0.62f, titlePaint.apply { textAlign = Paint.Align.CENTER })

        drawText(canvas, banner.title, w / 2f, h * 0.62f, 80f, banner.textColor, Paint.Align.CENTER, true)

        banner.subtitle?.let {
            drawText(canvas, it, w / 2f, h * 0.68f, 40f, 0xFFCCCCFF.toInt(), Paint.Align.CENTER)
        }

        banner.promotionText?.let {
            drawNeonBadge(canvas, it, w / 2f, h * 0.76f, banner.accentColor, w)
        }

        drawBottomBar(canvas, banner, w, h, Color.parseColor("#0A0A20"))
    }

    private fun drawMinimalElegant(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        canvas.drawColor(Color.WHITE)

        // Subtle gradient
        val grad = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.WHITE, Color.parseColor("#F0F0F0")), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = grad })

        // Poster
        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val destRect = RectF(0f, 0f, w.toFloat(), h * 0.60f)
                drawScaledBitmap(canvas, bmp, destRect)
            }
        }

        // Thin accent bar
        val accentPaint = Paint().apply {
            color = banner.accentColor
            strokeWidth = 8f
        }
        canvas.drawLine(80f, h * 0.64f, 200f, h * 0.64f, accentPaint)

        drawText(canvas, banner.title, 80f, h * 0.68f, 68f, Color.parseColor("#1A1A1A"), Paint.Align.LEFT, true, w - 160)

        banner.subtitle?.let {
            drawText(canvas, it, 80f, h * 0.73f, 36f, Color.parseColor("#666666"), Paint.Align.LEFT)
        }

        val meta = listOfNotNull(banner.genre, banner.year).joinToString("  |  ")
        drawText(canvas, meta, 80f, h * 0.77f, 30f, Color.parseColor("#999999"), Paint.Align.LEFT)

        banner.description?.let {
            drawMultilineText(canvas, it.take(200), 80f, h * 0.81f, 28f, Color.parseColor("#444444"), w - 160)
        }

        drawBottomBar(canvas, banner, w, h, Color.parseColor("#1A1A1A"))
    }

    private fun drawExplosiveAction(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        canvas.drawColor(Color.BLACK)

        // Full background poster
        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                drawScaledBitmap(canvas, bmp, RectF(0f, 0f, w.toFloat(), h.toFloat()))
            }
        }

        // Dark overlay
        val overlay = Paint().apply { color = Color.parseColor("#AA000000") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlay)

        // Bottom gradient
        val grad = LinearGradient(0f, h * 0.5f, 0f, h.toFloat(),
            intArrayOf(Color.TRANSPARENT, Color.BLACK), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.5f, w.toFloat(), h.toFloat(), Paint().apply { shader = grad })

        // Explosive diagonal accent
        val diagPaint = Paint().apply {
            color = banner.accentColor
            strokeWidth = 12f
            alpha = 180
        }
        canvas.drawLine(0f, h * 0.55f, w * 0.4f, h * 0.45f, diagPaint)

        // BIG TITLE
        drawText(canvas, banner.title.uppercase(), w / 2f, h * 0.72f, 90f, Color.WHITE, Paint.Align.CENTER, true, w - 80)

        banner.promotionText?.let {
            drawText(canvas, it.uppercase(), w / 2f, h * 0.80f, 60f, banner.accentColor, Paint.Align.CENTER, true)
        }

        banner.subtitle?.let {
            drawText(canvas, it, w / 2f, h * 0.85f, 38f, 0xFFCCCCCC.toInt(), Paint.Align.CENTER)
        }

        drawBottomBar(canvas, banner, w, h, Color.TRANSPARENT)
    }

    private fun drawSeriesBinge(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        val bgGrad = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(Color.parseColor("#1a1a2e"), Color.parseColor("#16213e"), Color.parseColor("#0f3460")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = bgGrad })

        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val margin = 80f
                val destRect = RectF(margin, h * 0.05f, w - margin, h * 0.50f)
                val rounded = getRoundedBitmap(bmp, 30f)
                drawScaledBitmap(canvas, rounded, destRect)
            }
        }

        // "SÉRIE" tag
        drawTag(canvas, "SÉRIE", w / 2f, h * 0.54f, banner.accentColor)

        drawText(canvas, banner.title, w / 2f, h * 0.61f, 76f, Color.WHITE, Paint.Align.CENTER, true, w - 120)

        banner.subtitle?.let {
            drawText(canvas, it, w / 2f, h * 0.67f, 36f, 0xFFAABBFF.toInt(), Paint.Align.CENTER)
        }

        val meta = listOfNotNull(banner.genre, banner.year).joinToString("  •  ")
        drawText(canvas, meta, w / 2f, h * 0.71f, 30f, 0xFF8899CC.toInt(), Paint.Align.CENTER)

        banner.description?.let {
            drawMultilineText(canvas, it.take(250), 80f, h * 0.75f, 28f, 0xFFCCCCDD.toInt(), w - 160)
        }

        banner.promotionText?.let {
            drawNeonBadge(canvas, it, w / 2f, h * 0.86f, banner.accentColor, w)
        }

        drawBottomBar(canvas, banner, w, h, Color.parseColor("#0A0A1A"))
    }

    private fun drawPromotionSale(canvas: Canvas, banner: Banner, w: Int, h: Int) {
        // Bold red/dark promo banner
        val grad = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#1a0000"), Color.parseColor("#3d0000")),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = grad })

        // Diagonal stripe pattern
        val stripePaint = Paint().apply {
            color = Color.parseColor("#22FFFFFF")
            strokeWidth = 40f
        }
        for (i in 0..20) {
            val x = i * 150f - 500f
            canvas.drawLine(x, 0f, x + h, h.toFloat(), stripePaint)
        }

        // Central logo area
        banner.posterImagePath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val cx = w / 2f
                val cy = h * 0.35f
                val radius = w * 0.35f
                val destRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                val circle = getCircleBitmap(bmp)
                canvas.drawBitmap(circle, null, destRect, null)
            }
        }

        // BIG PROMO TEXT
        banner.promotionText?.let {
            drawText(canvas, it.uppercase(), w / 2f, h * 0.62f, 120f, Color.WHITE, Paint.Align.CENTER, true)
        }

        // Yellow star/burst
        drawStarburst(canvas, w * 0.85f, h * 0.15f, 140f, Color.parseColor("#FFD700"))
        banner.promotionText?.let {
            drawText(canvas, it, w * 0.85f, h * 0.15f + 20, 40f, Color.parseColor("#1A0000"), Paint.Align.CENTER, true)
        }

        drawText(canvas, banner.title, w / 2f, h * 0.69f, 56f, 0xFFFFDDDD.toInt(), Paint.Align.CENTER, false, w - 80)

        banner.subtitle?.let {
            drawText(canvas, it, w / 2f, h * 0.74f, 36f, 0xFFFFAAAA.toInt(), Paint.Align.CENTER)
        }

        banner.description?.let {
            drawMultilineText(canvas, it.take(200), 80f, h * 0.78f, 28f, 0xFFFFCCCC.toInt(), w - 160)
        }

        drawBottomBar(canvas, banner, w, h, Color.parseColor("#0D0000"))
    }

    // ==================== HELPER DRAW METHODS ====================

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textSize: Float,
        color: Int,
        align: Paint.Align,
        bold: Boolean = false,
        maxWidth: Int = 0
    ) {
        val paint = TextPaint().apply {
            this.color = color
            this.textSize = textSize
            isFakeBoldText = bold
            textAlign = align
            isAntiAlias = true
        }
        if (maxWidth > 0 && paint.measureText(text) > maxWidth) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(2)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        } else {
            canvas.drawText(text, x, y, paint)
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textSize: Float,
        color: Int,
        maxWidth: Int
    ) {
        val paint = TextPaint().apply {
            this.color = color
            this.textSize = textSize
            isAntiAlias = true
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(4)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawPromotionBadge(canvas: Canvas, text: String, x: Float, y: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        val textPaint = TextPaint().apply {
            this.color = Color.WHITE
            textSize = 40f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val padding = 20f
        val textWidth = textPaint.measureText(text)
        val rect = RectF(x - textWidth / 2 - padding, y - 50f, x + textWidth / 2 + padding, y + 20f)
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawNeonBadge(canvas: Canvas, text: String, x: Float, y: Float, color: Int, w: Int) {
        val borderPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 4f
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
            isAntiAlias = true
        }
        val textPaint = TextPaint().apply {
            this.color = color
            textSize = 56f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val tw = textPaint.measureText(text)
        val padding = 30f
        val rect = RectF(x - tw / 2 - padding, y - 60f, x + tw / 2 + padding, y + 20f)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawTag(canvas: Canvas, text: String, x: Float, y: Float, color: Int) {
        val bgPaint = Paint().apply { this.color = color; isAntiAlias = true }
        val textPaint = TextPaint().apply {
            this.color = Color.WHITE
            textSize = 30f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        val tw = textPaint.measureText(text)
        val padding = 20f
        val rect = RectF(x - tw / 2 - padding, y - 35f, x + tw / 2 + padding, y + 12f)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawRating(canvas: Canvas, rating: Float, x: Float, y: Float, color: Int) {
        val stars = (rating / 2).coerceIn(0f, 5f)
        val paint = TextPaint().apply {
            this.color = color
            textSize = 36f
            isAntiAlias = true
        }
        val ratingText = "★".repeat(stars.toInt()) + "☆".repeat(5 - stars.toInt()) + "  ${String.format("%.1f", rating)}/10"
        canvas.drawText(ratingText, x, y, paint)
    }

    private fun drawStarburst(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val path = Path()
        val points = 12
        val innerRadius = radius * 0.5f
        for (i in 0 until points * 2) {
            val angle = Math.PI * i / points - Math.PI / 2
            val r = if (i % 2 == 0) radius else innerRadius
            val px = cx + (r * Math.cos(angle)).toFloat()
            val py = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, Paint().apply { this.color = color; isAntiAlias = true })
    }

    private fun drawBottomBar(canvas: Canvas, banner: Banner, w: Int, h: Int, bgColor: Int) {
        val barHeight = 180f
        val barTop = h - barHeight

        if (bgColor != Color.TRANSPARENT) {
            canvas.drawRect(0f, barTop, w.toFloat(), h.toFloat(), Paint().apply { color = bgColor })
        }

        // Divider line
        canvas.drawLine(0f, barTop, w.toFloat(), barTop, Paint().apply {
            color = banner.accentColor
            strokeWidth = 3f
        })

        // Logo on left
        banner.logoPath?.let { path ->
            loadBitmapSync(path)?.let { bmp ->
                val logoSize = 120f
                val logoRect = RectF(40f, barTop + 30f, 40f + logoSize, barTop + 30f + logoSize)
                canvas.drawBitmap(bmp, null, logoRect, null)
            }
        }

        // Server name
        banner.serverName?.let { name ->
            if (name.isNotBlank()) {
                drawText(canvas, name, if (banner.logoPath != null) 180f else 40f,
                    barTop + 80f, 36f, Color.WHITE, Paint.Align.LEFT, true)
            }
        }

        // Contact info on right
        banner.contactInfo?.let { contact ->
            if (contact.isNotBlank()) {
                drawText(canvas, contact, w - 40f, barTop + 80f, 32f, 0xFFCCCCCC.toInt(), Paint.Align.RIGHT)
            }
        }

        // "Gerador Plus" watermark
        drawText(canvas, "Gerador Plus", w / 2f, h - 20f, 24f, 0x55FFFFFF, Paint.Align.CENTER)
    }

    private fun drawScaledBitmap(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val srcAspect = bitmap.width.toFloat() / bitmap.height
        val dstAspect = destRect.width() / destRect.height()

        val srcRect: Rect
        if (srcAspect > dstAspect) {
            val newWidth = (bitmap.height * dstAspect).toInt()
            val offsetX = (bitmap.width - newWidth) / 2
            srcRect = Rect(offsetX, 0, offsetX + newWidth, bitmap.height)
        } else {
            val newHeight = (bitmap.width / dstAspect).toInt()
            val offsetY = (bitmap.height - newHeight) / 2
            srcRect = Rect(0, offsetY, bitmap.width, offsetY + newHeight)
        }
        canvas.drawBitmap(bitmap, srcRect, destRect, Paint().apply { isFilterBitmap = true })
    }

    private fun getRoundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val offsetX = (bitmap.width - size) / 2
        val offsetY = (bitmap.height - size) / 2
        canvas.drawBitmap(bitmap, -offsetX.toFloat(), -offsetY.toFloat(), paint)
        return output
    }

    private fun loadBitmapSync(path: String): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load(path)
                .submit(800, 1200)
                .get()
        } catch (e: Exception) {
            null
        }
    }

    private fun saveBitmap(bitmap: Bitmap, bannerId: Long): String? {
        return try {
            val dir = File(context.getExternalFilesDir(null), "GeradorPlus/banners")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "banner_${bannerId}_$timestamp.jpg")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getOutputDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "GeradorPlus/banners")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
