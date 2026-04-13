package com.geradorplus.data.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.geradorplus.data.models.*
import kotlinx.coroutines.flow.Flow

// ==================== TYPE CONVERTERS ====================

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.valueOf(role)

    @TypeConverter
    fun fromLicenseType(type: LicenseType): String = type.name

    @TypeConverter
    fun toLicenseType(type: String): LicenseType = LicenseType.valueOf(type)

    @TypeConverter
    fun fromLicenseStatus(status: LicenseStatus): String = status.name

    @TypeConverter
    fun toLicenseStatus(status: String): LicenseStatus = LicenseStatus.valueOf(status)

    @TypeConverter
    fun fromBannerType(type: BannerType): String = type.name

    @TypeConverter
    fun toBannerType(type: String): BannerType = BannerType.valueOf(type)

    @TypeConverter
    fun fromBannerTemplate(template: BannerTemplate): String = template.name

    @TypeConverter
    fun toBannerTemplate(template: String): BannerTemplate = BannerTemplate.valueOf(template)
}

// ==================== USER DAO ====================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE createdBy = :masterId ORDER BY createdAt DESC")
    fun getUsersByMaster(masterId: Long): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'MASTER' LIMIT 1")
    suspend fun getMasterUser(): User?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET licenseStatus = :status WHERE id = :userId")
    suspend fun updateLicenseStatus(userId: Long, status: LicenseStatus)

    @Query("UPDATE users SET expiresAt = :expiresAt, activatedAt = :activatedAt, licenseStatus = 'ACTIVE' WHERE id = :userId")
    suspend fun activateLicense(userId: Long, expiresAt: Long, activatedAt: Long)

    @Query("UPDATE users SET serverLogo = :logoPath WHERE id = :userId")
    suspend fun updateLogo(userId: Long, logoPath: String)

    @Query("UPDATE users SET serverName = :name, contactInfo = :contact WHERE id = :userId")
    suspend fun updateServerInfo(userId: Long, name: String, contact: String)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'MASTER'")
    suspend fun getMasterCount(): Int
}

// ==================== BANNER DAO ====================

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBannersByUser(userId: Long): Flow<List<Banner>>

    @Query("SELECT * FROM banners WHERE id = :id")
    suspend fun getBannerById(id: Long): Banner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner): Long

    @Update
    suspend fun updateBanner(banner: Banner)

    @Delete
    suspend fun deleteBanner(banner: Banner)

    @Query("DELETE FROM banners WHERE userId = :userId")
    suspend fun deleteAllBannersByUser(userId: Long)

    @Query("SELECT * FROM banners WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getBannersByType(userId: Long, type: BannerType): Flow<List<Banner>>
}

// ==================== DATABASE ====================

@Database(
    entities = [User::class, Banner::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bannerDao(): BannerDao

    companion object {
        const val DATABASE_NAME = "geradorplus_db"
    }
}
