package com.geradorplus.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.geradorplus.R
import com.geradorplus.data.models.*
import com.geradorplus.databinding.*
import com.geradorplus.ui.activities.BannerEditorActivity
import com.geradorplus.ui.activities.LoginActivity
import com.geradorplus.ui.adapters.*
import com.geradorplus.ui.viewmodels.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

// ==================== HOME FRAGMENT ====================

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BannerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUser()
        setupMoviesRecycler()
        setupSeriesRecycler()
        setupQuickActions()
    }

    private fun setupUser() {
        val user = viewModel.currentUser
        binding.tvWelcome.text = "Olá, ${user?.username ?: "Usuário"}!"
        binding.tvLicenseStatus.text = user?.getStatusLabel() ?: ""
        binding.tvLicenseStatus.setTextColor(
            if (user?.isExpired() == true) 0xFFFF4444.toInt() else 0xFF44FF88.toInt()
        )
        user?.let {
            if (it.licenseType == LicenseType.TRIAL) {
                binding.cardTrialWarning.visibility = View.VISIBLE
                binding.tvTrialTime.text = "Teste: ${it.getRemainingHours()}h restantes"
            }
            binding.tvActivatedAt.text = "Ativado em: ${formatDate(it.activatedAt)}"
            binding.tvExpiresAt.text = "Expira em: ${formatDate(it.expiresAt)}"
        }
    }

    private fun setupMoviesRecycler() {
        val adapter = ContentCardAdapter(emptyList(), "") { content ->
            openBannerEditor(content, content.isMovie())
        }
        binding.rvMovies.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMovies.adapter = adapter

        viewModel.trendingMovies.observe(viewLifecycleOwner) { movies ->
            val imageBase = viewModel.getImageUrl("")
            adapter.updateDataWithBase(movies, imageBase)
        }
    }

    private fun setupSeriesRecycler() {
        val adapter = ContentCardAdapter(emptyList(), "") { content ->
            openBannerEditor(content, false)
        }
        binding.rvSeries.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSeries.adapter = adapter

        viewModel.trendingSeries.observe(viewLifecycleOwner) { series ->
            val imageBase = viewModel.getImageUrl("")
            adapter.updateDataWithBase(series, imageBase)
        }
    }

    private fun setupQuickActions() {
        binding.btnCreateBanner.setOnClickListener {
            startActivity(Intent(requireContext(), BannerEditorActivity::class.java))
        }
        binding.btnCreatePromo.setOnClickListener {
            startActivity(Intent(requireContext(), BannerEditorActivity::class.java).apply {
                putExtra("BANNER_TYPE", BannerType.PROMOTION.name)
            })
        }
    }

    private fun openBannerEditor(content: TmdbContent, isMovie: Boolean) {
        startActivity(Intent(requireContext(), BannerEditorActivity::class.java).apply {
            putExtra("TMDB_ID", content.id)
            putExtra("IS_MOVIE", isMovie)
            putExtra("TMDB_TITLE", content.getDisplayTitle())
            putExtra("TMDB_YEAR", content.getYear())
            putExtra("TMDB_POSTER", content.posterPath ?: "")
            putExtra("TMDB_BACKDROP", content.backdropPath ?: "")
            putExtra("TMDB_OVERVIEW", content.overview ?: "")
            putExtra("TMDB_RATING", content.voteAverage ?: 0f)
        })
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== BANNERS FRAGMENT ====================

@AndroidEntryPoint
class BannersFragment : Fragment() {

    private var _binding: FragmentBannersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BannerViewModel by viewModels()
    private lateinit var adapter: BannerListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBannersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BannerListAdapter(
            onShare = { banner -> shareBanner(banner) },
            onDelete = { banner -> confirmDelete(banner) }
        )
        binding.rvBanners.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBanners.adapter = adapter

        viewModel.userBanners?.observe(viewLifecycleOwner) { banners ->
            adapter.submitList(banners)
            binding.tvEmpty.visibility = if (banners.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabCreate.setOnClickListener {
            startActivity(Intent(requireContext(), BannerEditorActivity::class.java))
        }
    }

    private fun shareBanner(banner: Banner) {
        banner.outputPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.fileprovider", file
                )
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Compartilhar"))
            }
        }
    }

    private fun confirmDelete(banner: Banner) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Banner")
            .setMessage("Deseja excluir o banner \"${banner.title}\"?")
            .setPositiveButton("Excluir") { _, _ -> viewModel.deleteBanner(banner) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== SEARCH FRAGMENT ====================

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BannerViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter
    private var currentSearchType = SearchType.ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchResultAdapter("") { content ->
            openBannerEditor(content)
        }
        binding.rvResults.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvResults.adapter = adapter

        setupSearch()
        setupTypeFilter()

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            val imageBase = viewModel.getImageUrl("")
            adapter.updateWithBase(results, imageBase)
            binding.tvNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressSearch.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    // ✅ CORRIGIDO: usa SearchView (searchView) ao invés de etSearch/btnSearch
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query?.trim() ?: ""
                if (q.isNotEmpty()) viewModel.searchContent(q, currentSearchType)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText?.trim() ?: ""
                if (q.length >= 2) viewModel.searchContent(q, currentSearchType)
                return true
            }
        })
    }

    private fun setupTypeFilter() {
        binding.chipAll.setOnClickListener { currentSearchType = SearchType.ALL; reSearch() }
        binding.chipMovies.setOnClickListener { currentSearchType = SearchType.MOVIE; reSearch() }
        binding.chipSeries.setOnClickListener { currentSearchType = SearchType.SERIES; reSearch() }
    }

    // ✅ CORRIGIDO: usa searchView.query ao invés de etSearch.text
    private fun reSearch() {
        val query = binding.searchView.query.toString().trim()
        if (query.isNotEmpty()) viewModel.searchContent(query, currentSearchType)
    }

    private fun openBannerEditor(content: TmdbContent) {
        startActivity(Intent(requireContext(), BannerEditorActivity::class.java).apply {
            putExtra("TMDB_ID", content.id)
            putExtra("IS_MOVIE", content.isMovie())
            putExtra("TMDB_TITLE", content.getDisplayTitle())
            putExtra("TMDB_YEAR", content.getYear())
            putExtra("TMDB_POSTER", content.posterPath ?: "")
            putExtra("TMDB_BACKDROP", content.backdropPath ?: "")
            putExtra("TMDB_OVERVIEW", content.overview ?: "")
            putExtra("TMDB_RATING", content.voteAverage ?: 0f)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== PROFILE FRAGMENT ====================

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private val pickLogoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = copyUriToCache(uri)
                path?.let {
                    profileViewModel.updateLogo(it)
                    Glide.with(this).load(it).into(binding.ivLogo)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        setupClickListeners()
        setupObservers()
    }

    private fun loadUserData() {
        val user = profileViewModel.currentUser ?: return
        binding.etServerName.setText(user.serverName ?: "")
        binding.etContact.setText(user.contactInfo ?: "")
        binding.tvUsername.text = user.username
        binding.tvRole.text = if (user.role == UserRole.MASTER) "Master" else "Usuário"
        binding.tvLicenseStatus.text = user.getStatusLabel()
        user.serverLogo?.let {
            Glide.with(this).load(it).into(binding.ivLogo)
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveInfo.setOnClickListener {
            val name = binding.etServerName.text.toString().trim()
            val contact = binding.etContact.text.toString().trim()
            profileViewModel.updateServerInfo(name, contact)
        }
        binding.btnChangeLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickLogoLauncher.launch(intent)
        }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupObservers() {
        profileViewModel.updateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OperationState.Success -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                is OperationState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etNew = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPassword)
        val etConfirm = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)
        AlertDialog.Builder(requireContext())
            .setTitle("Alterar Senha")
            .setView(dialogView)
            .setPositiveButton("Alterar") { _, _ ->
                val newPass = etNew.text.toString()
                val confirm = etConfirm.text.toString()
                if (newPass == confirm) profileViewModel.changePassword("", newPass)
                else Toast.makeText(requireContext(), "Senhas não conferem", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(requireContext().cacheDir, "logo_${System.currentTimeMillis()}.png")
            file.outputStream().use { inputStream.copyTo(it) }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== ADMIN DASHBOARD FRAGMENT ====================

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = adminViewModel.currentUser
        binding.tvMasterName.text = "Master: ${user?.username}"

        adminViewModel.getUsers()?.observe(viewLifecycleOwner) { users ->
            binding.tvTotalUsers.text = users.size.toString()
            binding.tvActiveUsers.text = users.count { !it.isExpired() && it.isActive }.toString()
            binding.tvExpiredUsers.text = users.count { it.isExpired() }.toString()
            binding.tvTrialUsers.text = users.count { it.licenseType == LicenseType.TRIAL }.toString()
        }

        binding.btnManageUsers.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainer, UsersFragment())
                ?.addToBackStack(null)?.commit()
        }
        binding.btnCreateBanner.setOnClickListener {
            startActivity(Intent(requireContext(), BannerEditorActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            adminViewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== USERS FRAGMENT ====================

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private val adminViewModel: AdminViewModel by viewModels()
    private lateinit var adapter: UserListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserListAdapter(
            onRenew = { user -> showRenewDialog(user) },
            onSuspend = { user -> adminViewModel.suspendUser(user.id) },
            onActivate = { user -> adminViewModel.activateUser(user.id) },
            onDelete = { user -> confirmDeleteUser(user) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        adminViewModel.getUsers()?.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddUser.setOnClickListener { showCreateUserDialog() }

        adminViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OperationState.Success -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                is OperationState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun showCreateUserDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_user, null)
        val etUsername = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
        val etPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val etServerName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etServerName)
        val etContact = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etContact)
        val etDays = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDays)
        val radioTrial = view.findViewById<android.widget.RadioButton>(R.id.radioTrial)

        AlertDialog.Builder(requireContext())
            .setTitle("Criar Usuário")
            .setView(view)
            .setPositiveButton("Criar") { _, _ ->
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString()
                val serverName = etServerName.text.toString().trim()
                val contact = etContact.text.toString().trim()
                val days = etDays.text.toString().toIntOrNull() ?: 30
                val licenseType = if (radioTrial.isChecked) LicenseType.TRIAL else LicenseType.MONTHLY
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(requireContext(), "Preencha usuário e senha", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                adminViewModel.createUser(username, password, licenseType, days, serverName, contact)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRenewDialog(user: User) {
        val options = arrayOf("7 dias", "15 dias", "30 dias", "60 dias", "90 dias")
        val days = arrayOf(7, 15, 30, 60, 90)
        AlertDialog.Builder(requireContext())
            .setTitle("Renovar - ${user.username}")
            .setItems(options) { _, which -> adminViewModel.renewLicense(user.id, days[which]) }
            .show()
    }

    private fun confirmDeleteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Usuário")
            .setMessage("Excluir \"${user.username}\"?")
            .setPositiveButton("Excluir") { _, _ -> adminViewModel.deleteUser(user.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
