package com.geradorplus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.geradorplus.R
import com.geradorplus.data.models.UserRole
import com.geradorplus.databinding.ActivityLoginBinding
import com.geradorplus.ui.viewmodels.AuthViewModel
import com.geradorplus.ui.viewmodels.LoginState
import com.geradorplus.ui.viewmodels.SetupState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var isFirstRun = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFirstRun = intent.getBooleanExtra("FIRST_RUN", false)

        if (isFirstRun) {
            showSetupMode()
        }

        setupObservers()
        setupClickListeners()
    }

    private fun showSetupMode() {
        binding.tvTitle.text = "Configuração Inicial"
        binding.tvSubtitle.text = "Crie o login Master do aplicativo"
        binding.tvSetupHint.visibility = View.VISIBLE
        binding.btnLogin.text = "Criar Master"
        binding.checkboxRemember.visibility = View.GONE
    }

    private fun setupObservers() {
        authViewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showLoading(true)
                is LoginState.Success -> {
                    showLoading(false)
                    val intent = if (state.user.role == UserRole.MASTER) {
                        Intent(this, AdminActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                is LoginState.Expired -> {
                    showLoading(false)
                    showError("Sua licença expirou. Entre em contato com o administrador.")
                }
            }
        }

        authViewModel.setupState.observe(this) { state ->
            when (state) {
                is SetupState.Loading -> showLoading(true)
                is SetupState.Success -> {
                    showLoading(false)
                    val intent = Intent(this, AdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is SetupState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isBlank() || password.isBlank()) {
                showError("Preencha usuário e senha")
                return@setOnClickListener
            }

            if (isFirstRun) {
                authViewModel.setupMaster(username, password)
            } else {
                authViewModel.login(username, password, binding.checkboxRemember.isChecked)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
