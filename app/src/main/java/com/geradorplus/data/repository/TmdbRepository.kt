package com.geradorplus.data.repository

import com.geradorplus.BuildConfig
import com.geradorplus.data.api.TmdbApiService
import com.geradorplus.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String) : ApiResult<T>()
    class Loading<T> : ApiResult<T>()
}

@Singleton
class TmdbRepository @Inject constructor(
    private val apiService: TmdbApiService
) {
    private val apiKey = BuildConfig.TMDB_API_KEY
    val imageBaseUrl = BuildConfig.TMDB_IMAGE_URL

    suspend fun searchMovies(query: String): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.searchMovies(apiKey, query)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro ao buscar filmes: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun searchSeries(query: String): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.searchTvShows(apiKey, query)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro ao buscar séries: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun searchMulti(query: String): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.searchMulti(apiKey, query)
            if (response.isSuccessful) {
                val results = response.body()?.results?.filter {
                    it.mediaType == "movie" || it.mediaType == "tv"
                } ?: emptyList()
                ApiResult.Success(results)
            } else {
                ApiResult.Error("Erro na busca: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getMovieDetail(movieId: Int): ApiResult<TmdbMovieDetail> {
        return try {
            val response = apiService.getMovieDetail(movieId, apiKey)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Erro ao buscar detalhes")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getTvShowDetail(tvId: Int): ApiResult<TmdbMovieDetail> {
        return try {
            val response = apiService.getTvShowDetail(tvId, apiKey)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Erro ao buscar detalhes")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getNowPlayingMovies(): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.getNowPlayingMovies(apiKey)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getUpcomingMovies(): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.getUpcomingMovies(apiKey)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getPopularMovies(): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.getPopularMovies(apiKey)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getPopularSeries(): ApiResult<List<TmdbContent>> {
        return try {
            val response = apiService.getPopularSeries(apiKey)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.results ?: emptyList())
            } else {
                ApiResult.Error("Erro: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro de conexão")
        }
    }

    suspend fun getMovieTrailer(movieId: Int): String? {
        return try {
            val response = apiService.getMovieVideos(movieId, apiKey)
            if (response.isSuccessful) {
                response.body()?.results?.firstOrNull { it.isTrailer() && it.site == "YouTube" }?.getYoutubeUrl()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTvTrailer(tvId: Int): String? {
        return try {
            val response = apiService.getTvVideos(tvId, apiKey)
            if (response.isSuccessful) {
                response.body()?.results?.firstOrNull { it.isTrailer() && it.site == "YouTube" }?.getYoutubeUrl()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
