package com.geradorplus.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.geradorplus.data.models.*
import com.geradorplus.data.repository.*
import com.geradorplus.utils.BannerGenerator
import com.geradorplus.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== AUTH VIEW MODEL ====================

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _setupState = MutableLiveData<SetupState>()
    val setupState: LiveData<SetupState> = _setupState

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val user = userRepository.login(username, password)
            when {
                user == null -> _loginState.value = LoginState.Error("Usuário ou senha incorretos")
                !user.isActive -> _loginState.value = LoginState.Error("Conta desativada")
                user.isExpired() -> _loginState.value = LoginState.Expired(user)
                else -> {
                    sessionManager.saveSession(user, rememberMe)
                    _loginState.value = LoginState.Success(user)
                }
            }
        }
    }

    fun setupMaster(username: String, password: String) {
        viewModelScope.launch {
            _setupState.value = SetupState.Loading
            if (username.isBlank() || password.length < 6) {
                _setupState.value = SetupState.Error("Usuário e senha (mín. 6 chars) obrigatórios")
                return@launch
            }
            val id = userRepository.createMasterUser(username, password)
            if (id > 0) {
                val user = userRepository.getUserById(id)
                if (user != null) {
                    sessionManager.saveSession(user)
                    _setupState.value = SetupState.Success(user)
                } else {
                    _setupState.value = SetupState.Error("Erro ao criar master")
                }
            } else {
                _setupState.value = SetupState.Error("Erro ao criar usuário")
            }
        }
    }

    fun checkSession(): User? = if (sessionManager.isLoggedIn()) sessionManager.getLoggedUser() else null

    fun hasMasterUser(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(userRepository.hasMasterUser())
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
    data class Expired(val user: User) : LoginState()
}

sealed class SetupState {
    object Loading : SetupState()
    data class Success(val user: User) : SetupState()
    data class Error(val message: String) : SetupState()
}

// ==================== ADMIN VIEW MODEL ====================

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _operationState = MutableLiveData<OperationState>()
    val operationState: LiveData<OperationState> = _operationState

    val currentUser get() = sessionManager.getLoggedUser()

    fun getUsers() = currentUser?.let {
        if (it.role == UserRole.MASTER) userRepository.getAllUsers()
        else userRepository.getUsersByMaster(it.id)
    }?.asLiveData()

    fun createUser(
        username: String,
        password: String,
        licenseType: LicenseType,
        durationDays: Int,
        serverName: String,
        contactInfo: String
    ) {
        viewModelScope.launch {
            val masterId = currentUser?.id ?: return@launch
            _operationState.value = OperationState.Loading
            val result = userRepository.createUser(
                username, password, licenseType, durationDays, masterId, serverName, contactInfo
            )
            result.fold(
                onSuccess = { _operationState.value = OperationState.Success("Usuário criado com sucesso!") },
                onFailure = { _operationState.value = OperationState.Error(it.message ?: "Erro") }
            )
        }
    }

    fun renewLicense(userId: Long, days: Int) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val success = userRepository.renewLicense(userId, days)
            if (success) {
                _operationState.value = OperationState.Success("Licença renovada por $days dias!")
            } else {
                _operationState.value = OperationState.Error("Erro ao renovar licença")
            }
        }
    }

    fun suspendUser(userId: Long) {
        viewModelScope.launch {
            userRepository.suspendUser(userId)
            _operationState.value = OperationState.Success("Usuário suspenso")
        }
    }

    fun activateUser(userId: Long) {
        viewModelScope.launch {
            userRepository.activateUser(userId)
            _operationState.value = OperationState.Success("Usuário ativado")
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            userRepository.deleteUser(userId)
            _operationState.value = OperationState.Success("Usuário removido")
        }
    }

    fun createMasterLogin(username: String, password: String) {
        viewModelScope.launch {
            if (currentUser?.role != UserRole.MASTER) {
                _operationState.value = OperationState.Error("Sem permissão")
                return@launch
            }
            _operationState.value = OperationState.Loading
            val id = userRepository.createMasterUser(username, password)
            if (id > 0) {
                _operationState.value = OperationState.Success("Login master criado!")
            } else {
                _operationState.value = OperationState.Error("Erro ao criar master")
            }
        }
    }
}

sealed class OperationState {
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

// ==================== BANNER VIEW MODEL ====================

@HiltViewModel
class BannerViewModel @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val tmdbRepository: TmdbRepository,
    private val bannerGenerator: BannerGenerator,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<TmdbContent>>()
    val searchResults: LiveData<List<TmdbContent>> = _searchResults

    private val _selectedContent = MutableLiveData<TmdbMovieDetail?>()
    val selectedContent: LiveData<TmdbMovieDetail?> = _selectedContent

    private val _generationState = MutableLiveData<GenerationState>()
    val generationState: LiveData<GenerationState> = _generationState

    private val _trendingMovies = MutableLiveData<List<TmdbContent>>()
    val trendingMovies: LiveData<List<TmdbContent>> = _trendingMovies

    private val _trendingSeries = MutableLiveData<List<TmdbContent>>()
    val trendingSeries: LiveData<List<TmdbContent>> = _trendingSeries

    val currentUser get() = sessionManager.getLoggedUser()

    val userBanners get() = currentUser?.let {
        bannerRepository.getBannersByUser(it.id).asLiveData()
    }

    init {
        loadTrending()
    }

    private fun loadTrending() {
        viewModelScope.launch {
            when (val result = tmdbRepository.getPopularMovies()) {
                is ApiResult.Success -> _trendingMovies.value = result.data ?: emptyList()
                else -> {}
            }
            when (val result = tmdbRepository.getPopularSeries()) {
                is ApiResult.Success -> _trendingSeries.value = result.data ?: emptyList()
                else -> {}
            }
        }
    }

    fun searchContent(query: String, type: SearchType = SearchType.ALL) {
        viewModelScope.launch {
            val result = when (type) {
                SearchType.MOVIE -> tmdbRepository.searchMovies(query)
                SearchType.SERIES -> tmdbRepository.searchSeries(query)
                SearchType.ALL -> tmdbRepository.searchMulti(query)
            }
            when (result) {
                is ApiResult.Success -> _searchResults.value = result.data ?: emptyList()
                is ApiResult.Error -> _searchResults.value = emptyList()
                else -> {}
            }
        }
    }

    fun selectContent(content: TmdbContent, isMovie: Boolean = true) {
        viewModelScope.launch {
            val result = if (isMovie) {
                tmdbRepository.getMovieDetail(content.id)
            } else {
                tmdbRepository.getTvShowDetail(content.id)
            }
            when (result) {
                is ApiResult.Success -> _selectedContent.value = result.data
                else -> _selectedContent.value = null
            }
        }
    }

    fun generateBanner(banner: Banner, isWide: Boolean = false) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading
            val outputPath = bannerGenerator.generateBanner(banner, isWide)
            if (outputPath != null) {
                val savedBanner = banner.copy(outputPath = outputPath)
                val id = bannerRepository.saveBanner(savedBanner)
                _generationState.value = GenerationState.Success(outputPath, id)
            } else {
                _generationState.value = GenerationState.Error("Erro ao gerar banner")
            }
        }
    }

    fun deleteBanner(banner: Banner) {
        viewModelScope.launch {
            bannerRepository.deleteBanner(banner)
        }
    }

    fun getImageUrl(path: String): String = tmdbRepository.imageBaseUrl + path
}

enum class SearchType { MOVIE, SERIES, ALL }

sealed class GenerationState {
    object Loading : GenerationState()
    data class Success(val filePath: String, val bannerId: Long) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

// ==================== PROFILE VIEW MODEL ====================

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _updateState = MutableLiveData<OperationState>()
    val updateState: LiveData<OperationState> = _updateState

    val currentUser get() = sessionManager.getLoggedUser()

    fun updateServerInfo(name: String, contact: String) {
        viewModelScope.launch {
            val userId = currentUser?.id ?: return@launch
            userRepository.updateServerInfo(userId, name, contact)
            val updated = userRepository.getUserById(userId)
            updated?.let { sessionManager.updateUser(it) }
            _updateState.value = OperationState.Success("Informações salvas!")
        }
    }

    fun updateLogo(logoPath: String) {
        viewModelScope.launch {
            val userId = currentUser?.id ?: return@launch
            userRepository.updateLogo(userId, logoPath)
            val updated = userRepository.getUserById(userId)
            updated?.let { sessionManager.updateUser(it) }
            _updateState.value = OperationState.Success("Logo atualizada!")
        }
    }

    fun changePassword(current: String, newPassword: String) {
        viewModelScope.launch {
            val userId = currentUser?.id ?: return@launch
            if (newPassword.length < 6) {
                _updateState.value = OperationState.Error("Senha deve ter mínimo 6 caracteres")
                return@launch
            }
            val success = userRepository.changePassword(userId, newPassword)
            if (success) {
                _updateState.value = OperationState.Success("Senha alterada!")
            } else {
                _updateState.value = OperationState.Error("Erro ao alterar senha")
            }
        }
    }
}
