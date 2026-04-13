package com.geradorplus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.geradorplus.R
import com.geradorplus.data.models.UserRole
import com.geradorplus.databinding.ActivitySplashBinding
import com.geradorplus.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 2000)
    }

    private fun checkSession() {
        val user = authViewModel.checkSession()
        if (user != null) {
            // Auto-login with saved session
            val intent = if (user.role == UserRole.MASTER) {
                Intent(this, AdminActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        } else {
            // Check if first run (no master)
            authViewModel.hasMasterUser { hasMaster ->
                runOnUiThread {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        putExtra("FIRST_RUN", !hasMaster)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
