package com.example.launcher.system

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.launcher.model.SystemSyncStatus
import java.security.MessageDigest

object SystemSyncChecker {

    fun checkStatus(context: Context): SystemSyncStatus {
        return SystemSyncStatus(
            isDefaultLauncher = isDefaultLauncher(context),
            isNotificationListenerEnabled = isNotificationListenerEnabled(context),
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context),
            isOverlayPermissionGranted = Settings.canDrawOverlays(context),
            isWriteSettingsGranted = Settings.System.canWrite(context),
            isDndPolicyGranted = isDndPolicyGranted(context)
        )
    }

    fun isDefaultLauncher(context: Context): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == context.packageName
        }.getOrDefault(false)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        return runCatching {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val expected = ComponentName(context, VitroNotificationListenerService::class.java).flattenToString()
            flat.contains(expected) || flat.contains(context.packageName)
        }.getOrDefault(false)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (VitroAccessibilityService.isRunning.value) return true
        return runCatching {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val expected = ComponentName(context, VitroAccessibilityService::class.java).flattenToString()
            enabled.contains(expected, ignoreCase = true) ||
                enabled.contains("${context.packageName}/", ignoreCase = true)
        }.getOrDefault(false)
    }

    fun isDndPolicyGranted(context: Context): Boolean {
        return runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.isNotificationPolicyAccessGranted == true
        }.getOrDefault(false)
    }

    fun openDefaultLauncherSettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    fun openNotificationListenerSettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAccessibilitySettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        launchSafe(context, intent)
    }

    fun openWriteSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        launchSafe(context, intent)
    }

    fun openDndPolicySettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun launchSafe(context: Context, intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** Salted SHA-256 hash so lock screen PIN / password is never stored as plaintext. */
    fun hashPasscode(raw: String): String {
        if (raw.isEmpty()) return ""
        val salted = "vitro_lock_salt_v2:$raw"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPasscode(rawInput: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return true
        return hashPasscode(rawInput) == expectedHash
    }
}
