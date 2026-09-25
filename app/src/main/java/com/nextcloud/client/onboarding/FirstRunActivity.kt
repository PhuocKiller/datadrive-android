/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.FirstRunActivityBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * DataDrive welcome screen: logo, tagline, "Log in" and "Home Page". The language is chosen in Settings.
 */
class FirstRunActivity :
    BaseActivity(),
    Injectable {

    @JvmField
    @Inject
    var userAccountManager: UserAccountManager? = null

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    @JvmField
    @Inject
    var onboarding: OnboardingService? = null

    @JvmField
    @Inject
    var viewThemeUtilsFactory: ViewThemeUtils.Factory? = null

    private var activityResult: ActivityResultLauncher<Intent>? = null

    private lateinit var binding: FirstRunActivityBinding
    private var defaultViewThemeUtils: ViewThemeUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableAccountHandling = false

        super.onCreate(savedInstanceState)

        applyDefaultTheme()

        binding = FirstRunActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerActivityResult()
        setupLoginButton()
        setupHomePageButton()
        deleteAccountAtFirstLaunch()
        handleOnBackPressed()
    }

    private fun applyDefaultTheme() {
        defaultViewThemeUtils = viewThemeUtilsFactory?.withPrimaryAsBackground()
        defaultViewThemeUtils?.platform?.colorStatusBar(this, resources.getColor(R.color.white))
    }

    private fun registerActivityResult() {
        activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (RESULT_OK == result.resultCode) {
                    val data = result.data
                    val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    val account = userAccountManager?.getAccountByName(accountName)
                    if (account == null) {
                        DisplayUtils.showSnackMessage(this, R.string.account_creation_failed)
                        return@registerForActivityResult
                    }

                    userAccountManager?.setCurrentOwnCloudAccount(account.name)

                    val i = Intent(this, FileDisplayActivity::class.java)
                    i.action = FileDisplayActivity.RESTART
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    finish()
                }
            }
    }

    private fun setupLoginButton() {
        binding.login.setOnClickListener {
            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                activityResult?.launch(Intent(this, AuthenticatorActivity::class.java))
            } else {
                // started for result by AuthenticatorActivity, which starts the web login once we return
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun setupHomePageButton() {
        binding.homePage.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.url_home_page).toUri()))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
    private fun deleteAccountAtFirstLaunch() {
        if (onboarding?.isFirstRun == true) {
            userAccountManager?.removeAllAccounts()
        }
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isFromAddAccount = intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)

                    val destination: Intent = if (isFromAddAccount) {
                        Intent(applicationContext, FileDisplayActivity::class.java)
                    } else {
                        Intent(applicationContext, AuthenticatorActivity::class.java)
                    }

                    if (!isFromAddAccount) {
                        destination.putExtra(EXTRA_EXIT, true)
                    }

                    destination.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(destination)
                    finish()
                }
            }
        )
    }

    private fun onFinish() {
        preferences?.lastSeenVersionCode = BuildConfig.VERSION_CODE
    }

    override fun onStop() {
        onFinish()
        super.onStop()
    }

    companion object {
        const val EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE"
        const val EXTRA_EXIT = "EXIT"
    }
}
