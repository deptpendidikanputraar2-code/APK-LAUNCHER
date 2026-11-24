package com.example.launcher.system

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class VitroAccessibilityService : AccessibilityService() {

    companion object {
        private var instanceRef: WeakReference<VitroAccessibilityService>? = null

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun lockPhoneScreen(): Boolean {
            val service = instanceRef?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }

        fun takeScreenshot(): Boolean {
            val service = instanceRef?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isRunning.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Lightweight passive listener; only global actions (Lock Screen & Screenshot) are used.
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        _isRunning.value = false
        instanceRef = null
        super.onDestroy()
    }
}
