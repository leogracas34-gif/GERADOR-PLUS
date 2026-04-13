package com.geradorplus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.geradorplus.R
import com.geradorplus.databinding.ActivityMainBinding
import com.geradorplus.ui.fragments.*
import com.geradorplus.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_banners -> { loadFragment(BannersFragment()); true }
                R.id.nav_search -> { loadFragment(SearchFragment()); true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun openBannerEditor(tmdbId: Int? = null, isMovie: Boolean = true) {
        val intent = Intent(this, BannerEditorActivity::class.java).apply {
            putExtra("TMDB_ID", tmdbId)
            putExtra("IS_MOVIE", isMovie)
        }
        startActivity(intent)
    }
}
