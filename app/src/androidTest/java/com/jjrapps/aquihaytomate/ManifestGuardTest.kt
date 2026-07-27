package com.jjrapps.aquihaytomate

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the manifest rules from CLAUDE.md §3 that would only be caught by a Play rejection
 * otherwise, which is far too late.
 */
@RunWith(AndroidJUnit4::class)
class ManifestGuardTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val declaredPermissions: List<String>
        get() = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toList()
            .orEmpty()

    @Test
    fun useExactAlarmIsNeverDeclared() {
        assertFalse(
            "USE_EXACT_ALARM is reserved by Play for alarm clocks and calendars. " +
                "The exact alarm is only a backstop here; see CLAUDE.md §3.",
            declaredPermissions.contains("android.permission.USE_EXACT_ALARM"),
        )
    }

    @Test
    fun fullScreenIntentIsNeverDeclared() {
        assertFalse(
            "USE_FULL_SCREEN_INTENT is restricted to calls and alarm clocks from Android 14.",
            declaredPermissions.contains("android.permission.USE_FULL_SCREEN_INTENT"),
        )
    }

    @Test
    fun internetIsNeverDeclared() {
        assertFalse(
            "The app is fully offline and the Play data safety form says so.",
            declaredPermissions.contains("android.permission.INTERNET"),
        )
    }

    @Test
    fun foregroundServiceSpecialUseIsDeclared() {
        assertTrue(
            "The timer needs FOREGROUND_SERVICE_SPECIAL_USE; see " +
                "docs/decisions/002-motor-del-temporizador-hibrido.md",
            declaredPermissions.contains("android.permission.FOREGROUND_SERVICE_SPECIAL_USE"),
        )
    }
}
