package com.ultimatecloner.safe.Utils

import android.content.Context
import android.content.SharedPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Play Store REQUIREMENT: Privacy Policy + Terms must be shown to users.
 */
object PolicyUtils {
    
    private const val PREFS_NAME = "policy_prefs"
    private const val KEY_POLICY_ACCEPTED = "policy_accepted"
    private const val POLICY_VERSION = 1
    
    fun showPrivacyPolicyOnFirstLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accepted = prefs.getInt(KEY_POLICY_ACCEPTED, 0)
        
        if (accepted < POLICY_VERSION) {
            MaterialAlertDialogBuilder(context)
                .setTitle("Privacy Policy & Terms")
                .setMessage("""
                    Container Pro Privacy Policy:
                    
                    1. We do NOT collect or store any personal information.
                    2. We do NOT modify or distribute third-party applications.
                    3. This app creates isolated environments for apps you already have installed.
                    4. All data stays on your device.
                    5. We use Firebase Analytics for anonymous usage statistics ONLY.
                    
                    By accepting, you agree to these terms.
                """.trimIndent())
                .setPositiveButton("Accept") { _, _ ->
                    prefs.edit().putInt(KEY_POLICY_ACCEPTED, POLICY_VERSION).apply()
                }
                .setCancelable(false)
                .show()
        }
    }
}
