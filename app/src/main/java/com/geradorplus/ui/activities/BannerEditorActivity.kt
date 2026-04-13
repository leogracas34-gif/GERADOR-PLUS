package com.geradorplus.ui.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.geradorplus.R
import com.geradorplus.data.models.*
import com.geradorplus.databinding.ActivityBannerEditorBinding
import com.geradorplus.ui.viewmodels.BannerViewModel
import com.geradorplus.ui.viewmodels.GenerationState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class BannerEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBannerEditorBinding
    private val viewModel: BannerViewModel by viewModels()

    private var selectedPosterPath: String? = null
    private var selectedTemplate = BannerTemplate.DARK_CINEMA
    private var selectedBannerType = BannerType.MOVIE_LAUNCH
    private var selectedPrimaryColor = 0xFF1A1A2E.toInt()
    private var selectedAccentColor = 0xFFE94560.toInt()
    private var isWideFormat = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = getRealPathFromUri(uri)
                selectedPosterPath = path
                Glide.with(this).load(uri).into(binding.ivPosterPreview)
                binding.ivPosterPreview.visibility = View.VISIBLE
            }
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) pickImage() else Toast.makeText(this, "Permissão necessária para selecionar imagens", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannerEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTemplateSelector()
        setupTypeSelector()
        setupClickListeners()
        setupObservers()

        // Load from TMDB if provided
        intent.getIntExtra("TMDB_ID", -1).let { tmdbId ->
            if (tmdbId > 0) {
                val isMovie = intent.getBooleanExtra("IS_MOVIE", true)
                // Pre-fill from TMDB data
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Criar Banner"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTemplateSelector() {
        val templates = BannerTemplate.values()
        val templateNames = mapOf(
            BannerTemplate.DARK_CINEMA to "Cinema Dark",
            BannerTemplate.NEON_GLOW to "Neon Glow",
            BannerTemplate.MINIMAL_ELEGANT to "Minimal",
            BannerTemplate.EXPLOSIVE_ACTION to "Explosivo",
            BannerTemplate.SERIES_BINGE to "Séries",
            BannerTemplate.PROMOTION_SALE to "Promoção"
        )
        val templateIcons = mapOf(
            BannerTemplate.DARK_CINEMA to R.drawable.ic_template_cinema,
            BannerTemplate.NEON_GLOW to R.drawable.ic_template_neon,
            BannerTemplate.MINIMAL_ELEGANT to R.drawable.ic_template_minimal,
            BannerTemplate.EXPLOSIVE_ACTION to R.drawable.ic_template_action,
            BannerTemplate.SERIES_BINGE to R.drawable.ic_template_series,
            BannerTemplate.PROMOTION_SALE to R.drawable.ic_template_promo
        )

        binding.chipGroupTemplate.removeAllViews()
        templates.forEach { template ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = templateNames[template]
                isCheckable = true
                isChecked = template == selectedTemplate
                setOnClickListener {
                    selectedTemplate = template
                    updatePreviewHint()
                }
            }
            binding.chipGroupTemplate.addView(chip)
        }
    }

    private fun setupTypeSelector() {
        val types = listOf(
            BannerType.MOVIE_LAUNCH to "🎬 Filme",
            BannerType.SERIES_LAUNCH to "📺 Série",
            BannerType.PROMOTION to "🔥 Promoção",
            BannerType.TRAILER to "▶️ Trailer",
            BannerType.IPTV_SERVICE to "📡 IPTV",
            BannerType.CUSTOM to "✏️ Custom"
        )
        types.forEach { (type, label) ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = type == selectedBannerType
                setOnClickListener {
                    selectedBannerType = type
                    updateFieldsForType(type)
                }
            }
            binding.chipGroupType.addView(chip)
        }
    }

    private fun updateFieldsForType(type: BannerType) {
        when (type) {
            BannerType.PROMOTION -> {
                binding.tilPromoText.visibility = View.VISIBLE
                binding.tilSubtitle.hint = "Ex: Assine nosso IPTV agora!"
            }
            BannerType.MOVIE_LAUNCH, BannerType.SERIES_LAUNCH -> {
                binding.tilPromoText.visibility = View.VISIBLE
                binding.tilSubtitle.hint = "Subtítulo"
            }
            else -> {
                binding.tilSubtitle.hint = "Subtítulo"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectPoster.setOnClickListener { requestStoragePermission() }

        binding.btnColorAccent.setOnClickListener {
            showColorPicker { color ->
                selectedAccentColor = color
                binding.btnColorAccent.setBackgroundColor(color)
            }
        }

        binding.switchWideFormat.setOnCheckedChangeListener { _, isChecked ->
            isWideFormat = isChecked
            binding.tvFormatHint.text = if (isChecked) "Formato 16:9 (horizontal)" else "Formato 9:16 (stories)"
        }

        binding.btnGenerate.setOnClickListener {
            generateBanner()
        }
    }

    private fun generateBanner() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isBlank()) {
            binding.tilTitle.error = "Título obrigatório"
            return
        }

        val user = viewModel.currentUser ?: return

        val banner = Banner(
            userId = user.id,
            title = title,
            subtitle = binding.etSubtitle.text.toString().trim().ifBlank { null },
            description = binding.etDescription.text.toString().trim().ifBlank { null },
            type = selectedBannerType,
            template = selectedTemplate,
            posterImagePath = selectedPosterPath,
            logoPath = user.serverLogo,
            contactInfo = user.contactInfo ?: binding.etContact.text.toString().trim().ifBlank { null },
            serverName = user.serverName ?: binding.etServerName.text.toString().trim().ifBlank { null },
            primaryColor = selectedPrimaryColor,
            accentColor = selectedAccentColor,
            genre = binding.etGenre.text.toString().trim().ifBlank { null },
            year = binding.etYear.text.toString().trim().ifBlank { null },
            promotionText = binding.etPromoText.text.toString().trim().ifBlank { null },
            rating = binding.etRating.text.toString().toFloatOrNull()
        )

        viewModel.generateBanner(banner, isWideFormat)
    }

    private fun setupObservers() {
        viewModel.generationState.observe(this) { state ->
            when (state) {
                is GenerationState.Loading -> {
                    binding.progressGenerate.visibility = View.VISIBLE
                    binding.btnGenerate.isEnabled = false
                    binding.tvGenerateStatus.text = "Gerando banner..."
                }
                is GenerationState.Success -> {
                    binding.progressGenerate.visibility = View.GONE
                    binding.btnGenerate.isEnabled = true
                    binding.tvGenerateStatus.text = "Banner gerado com sucesso!"
                    showBannerResult(state.filePath)
                }
                is GenerationState.Error -> {
                    binding.progressGenerate.visibility = View.GONE
                    binding.btnGenerate.isEnabled = true
                    binding.tvGenerateStatus.text = state.message
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showBannerResult(filePath: String) {
        binding.cardResult.visibility = View.VISIBLE
        Glide.with(this).load(filePath).into(binding.ivResult)

        binding.btnShare.setOnClickListener {
            shareBanner(filePath)
        }

        binding.btnSaveGallery.setOnClickListener {
            saveBannerToGallery(filePath)
        }
    }

    private fun shareBanner(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar Banner"))
    }

    private fun saveBannerToGallery(filePath: String) {
        try {
            val file = File(filePath)
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GeradorPlus")
                }
            }
            val resolver = contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    file.inputStream().copyTo(out)
                }
                Toast.makeText(this, "Salvo na galeria!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) pickImage() else storagePermissionLauncher.launch(permissions)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "poster_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { inputStream.copyTo(it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun updatePreviewHint() {
        binding.tvTemplateHint.text = when (selectedTemplate) {
            BannerTemplate.DARK_CINEMA -> "Fundo escuro, estilo cinema"
            BannerTemplate.NEON_GLOW -> "Efeito neon vibrante"
            BannerTemplate.MINIMAL_ELEGANT -> "Design limpo e moderno"
            BannerTemplate.EXPLOSIVE_ACTION -> "Imagem de fundo total, impactante"
            BannerTemplate.SERIES_BINGE -> "Temática de séries"
            BannerTemplate.PROMOTION_SALE -> "Grande destaque em promoção"
        }
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        // Simple color selection dialog
        val colors = arrayOf(
            0xFFE94560.toInt(), 0xFF00B4D8.toInt(), 0xFF06D6A0.toInt(),
            0xFFFFB703.toInt(), 0xFF8338EC.toInt(), 0xFFFF006E.toInt(),
            0xFFFFFFFF.toInt(), 0xFF1A1A2E.toInt()
        )
        val colorNames = arrayOf("Vermelho", "Azul", "Verde", "Amarelo", "Roxo", "Rosa", "Branco", "Dark")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cor de Destaque")
            .setItems(colorNames) { _, which -> onColorSelected(colors[which]) }
            .show()
    }
}
