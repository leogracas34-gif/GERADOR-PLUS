package com.geradorplus.data.repository

import com.geradorplus.data.database.BannerDao
import com.geradorplus.data.models.Banner
import com.geradorplus.data.models.BannerType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerRepository @Inject constructor(
    private val bannerDao: BannerDao
) {
    fun getBannersByUser(userId: Long): Flow<List<Banner>> = bannerDao.getBannersByUser(userId)

    fun getBannersByType(userId: Long, type: BannerType): Flow<List<Banner>> =
        bannerDao.getBannersByType(userId, type)

    suspend fun saveBanner(banner: Banner): Long = bannerDao.insertBanner(banner)

    suspend fun updateBanner(banner: Banner) = bannerDao.updateBanner(banner)

    suspend fun deleteBanner(banner: Banner) = bannerDao.deleteBanner(banner)

    suspend fun getBannerById(id: Long): Banner? = bannerDao.getBannerById(id)
}
