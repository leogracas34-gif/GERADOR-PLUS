package com.geradorplus.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

// ==================== USER MODEL ====================

enum class UserRole { MASTER, USER }
enum class LicenseType { TRIAL, MONTHLY, NONE }
enum class LicenseStatus { ACTIVE, EXPIRED, SUSPENDED }

@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String, // hashed
    val role: UserRole = UserRole.USER,
    val licenseType: LicenseType = LicenseType.MONTHLY,
    val licenseStatus: LicenseStatus = LicenseStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // timestamp
    val activatedAt: Long = 0L,
    val serverLogo: String? = null, // path to logo file
    val serverName: String? = null,
    val contactInfo: String? = null, // WhatsApp, Telegram, etc.
    val isActive: Boolean = true,
    val createdBy: Long = 0L // master user id
) : Parcelable {

    fun isExpired(): Boolean {
        if (licenseType == LicenseType.TRIAL) {
            return System.currentTimeMillis() > expiresAt
        }
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt
    }

    fun getRemainingDays(): Long {
        if (expiresAt == 0L) return 0
        val diff = expiresAt - System.currentTimeMillis()
        return if (diff > 0) diff / (1000 * 60 * 60 * 24) else 0
    }

    fun getRemainingHours(): Long {
        if (expiresAt == 0L) return 0
        val diff = expiresAt - System.currentTimeMillis()
        return if (diff > 0) diff / (1000 * 60 * 60) else 0
    }

    fun getStatusLabel(): String {
        return when {
            isExpired() -> "EXPIRADO"
            licenseType == LicenseType.TRIAL -> "TESTE - ${getRemainingHours()}h restantes"
            else -> "ATIVO - ${getRemainingDays()} dias restantes"
        }
    }
}

// ==================== BANNER MODEL ====================

enum class BannerType {
    PROMOTION,
    MOVIE_LAUNCH,
    SERIES_LAUNCH,
    TRAILER,
    IPTV_SERVICE,
    CUSTOM
}

enum class BannerTemplate {
    DARK_CINEMA,
    NEON_GLOW,
    MINIMAL_ELEGANT,
    EXPLOSIVE_ACTION,
    SERIES_BINGE,
    PROMOTION_SALE
}

@Parcelize
@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val type: BannerType = BannerType.CUSTOM,
    val template: BannerTemplate = BannerTemplate.DARK_CINEMA,
    val backgroundImagePath: String? = null,
    val posterImagePath: String? = null,
    val logoPath: String? = null,
    val contactInfo: String? = null,
    val primaryColor: Int = 0xFF1A1A2E.toInt(),
    val accentColor: Int = 0xFFE94560.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val outputPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val tmdbId: Int? = null,
    val genre: String? = null,
    val year: String? = null,
    val rating: Float? = null,
    val promotionText: String? = null, // ex: "50% OFF", "ASSINE JÁ"
    val isVideo: Boolean = false,
    val serverName: String? = null
) : Parcelable

// ==================== TMDB MODELS ====================

data class TmdbSearchResponse(
    @SerializedName("results") val results: List<TmdbContent>,
    @SerializedName("total_results") val totalResults: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class TmdbContent(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?, // for series
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("media_type") val mediaType: String? = null
) {
    fun getDisplayTitle(): String = title ?: name ?: "Sem título"
    fun getDisplayDate(): String = releaseDate ?: firstAirDate ?: ""
    fun getYear(): String = getDisplayDate().take(4)
    fun getPosterUrl(baseUrl: String): String = if (!posterPath.isNullOrEmpty()) "$baseUrl$posterPath" else ""
    fun getBackdropUrl(baseUrl: String): String = if (!backdropPath.isNullOrEmpty()) "$baseUrl$backdropPath" else ""
    fun isMovie(): Boolean = !title.isNullOrEmpty() || mediaType == "movie"
}

data class TmdbMovieDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("tagline") val tagline: String?
) {
    fun getGenresText(): String = genres?.joinToString(", ") { it.name } ?: ""
    fun getDisplayTitle(): String = title ?: name ?: ""
    fun getYear(): String = (releaseDate ?: firstAirDate ?: "").take(4)
}

data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbVideosResponse(
    @SerializedName("results") val results: List<TmdbVideo>
)

data class TmdbVideo(
    @SerializedName("key") val key: String,
    @SerializedName("site") val site: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String
) {
    fun getYoutubeUrl(): String = "https://www.youtube.com/watch?v=$key"
    fun isTrailer(): Boolean = type.equals("Trailer", ignoreCase = true)
}

// ==================== SESSION MODEL ====================

data class Session(
    val user: User,
    val loginTime: Long = System.currentTimeMillis()
)
