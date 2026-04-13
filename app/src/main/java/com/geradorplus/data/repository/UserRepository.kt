package com.geradorplus.data.repository

import com.geradorplus.data.database.UserDao
import com.geradorplus.data.models.*
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    suspend fun login(username: String, password: String): User? {
        val hashedPassword = hashPassword(password)
        return userDao.login(username, hashedPassword)
    }

    suspend fun createMasterUser(username: String, password: String): Long {
        val existing = userDao.getMasterUser()
        if (existing != null) {
            // Already has a master - create another with master role
        }
        val user = User(
            username = username,
            password = hashPassword(password),
            role = UserRole.MASTER,
            licenseType = LicenseType.MONTHLY,
            licenseStatus = LicenseStatus.ACTIVE,
            expiresAt = Long.MAX_VALUE // Master never expires
        )
        return userDao.insertUser(user)
    }

    suspend fun createUser(
        username: String,
        password: String,
        licenseType: LicenseType,
        durationDays: Int = 30,
        masterId: Long,
        serverName: String = "",
        contactInfo: String = ""
    ): Result<Long> {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            return Result.failure(Exception("Usuário '$username' já existe"))
        }

        val now = System.currentTimeMillis()
        val expiresAt = when (licenseType) {
            LicenseType.TRIAL -> now + (60 * 60 * 1000L) // 1 hora
            LicenseType.MONTHLY -> now + (durationDays * 24 * 60 * 60 * 1000L)
            LicenseType.NONE -> 0L
        }

        val user = User(
            username = username,
            password = hashPassword(password),
            role = UserRole.USER,
            licenseType = licenseType,
            licenseStatus = LicenseStatus.ACTIVE,
            createdAt = now,
            activatedAt = now,
            expiresAt = expiresAt,
            createdBy = masterId,
            serverName = serverName,
            contactInfo = contactInfo
        )
        return try {
            val id = userDao.insertUser(user)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renewLicense(userId: Long, durationDays: Int): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val now = System.currentTimeMillis()
        // If already expired, renew from now; otherwise extend from current expiry
        val baseTime = if (user.isExpired()) now else maxOf(user.expiresAt, now)
        val newExpiry = baseTime + (durationDays * 24 * 60 * 60 * 1000L)
        userDao.activateLicense(userId, newExpiry, now)
        return true
    }

    suspend fun suspendUser(userId: Long) {
        userDao.updateLicenseStatus(userId, LicenseStatus.SUSPENDED)
    }

    suspend fun activateUser(userId: Long) {
        userDao.updateLicenseStatus(userId, LicenseStatus.ACTIVE)
    }

    fun getUsersByMaster(masterId: Long): Flow<List<User>> {
        return userDao.getUsersByMaster(masterId)
    }

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)

    suspend fun updateServerInfo(userId: Long, name: String, contact: String) {
        userDao.updateServerInfo(userId, name, contact)
    }

    suspend fun updateLogo(userId: Long, logoPath: String) {
        userDao.updateLogo(userId, logoPath)
    }

    suspend fun deleteUser(userId: Long) {
        val user = userDao.getUserById(userId) ?: return
        userDao.deleteUser(user)
    }

    suspend fun changePassword(userId: Long, newPassword: String): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val updated = user.copy(password = hashPassword(newPassword))
        userDao.updateUser(updated)
        return true
    }

    suspend fun hasMasterUser(): Boolean = userDao.getMasterCount() > 0

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
