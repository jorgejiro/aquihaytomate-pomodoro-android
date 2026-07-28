package com.jjrapps.aquihaytomate

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    /**
     * `specialUse` is the only type this app may declare, and it is the one whose justification was
     * submitted to Play Console. Any other type would be a policy violation found at review time.
     */
    @Test
    fun theTimerServiceOnlyDeclaresSpecialUse() {
        val service = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SERVICES)
            .services
            ?.firstOrNull { it.name.endsWith("PomodoroTimerService") }

        assertNotNull("PomodoroTimerService is not declared in the manifest", service)
        assertEquals(
            "Only specialUse is allowed; see CLAUDE.md §3",
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            service!!.foregroundServiceType,
        )
    }

    @Test
    fun theTimerServiceSurvivesASwipeFromRecents() {
        val service = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SERVICES)
            .services
            ?.first { it.name.endsWith("PomodoroTimerService") }

        assertEquals(
            "stopWithTask must be false or the countdown dies with the task",
            0,
            service!!.flags and ServiceInfo.FLAG_STOP_WITH_TASK,
        )
    }

    /**
     * The widget must not claim a configuration Activity.
     *
     * `android:configure` is not a boolean: it holds a class name. `android:configure="false"` compiled to
     * the literal class name `false`, the launcher tried to start it when the widget was dropped, and
     * Nova reported **"the app isn't installed"** — while still listing the widget in its picker. That was
     * a real bug in the shipped provider, and a string typo cannot be caught by the compiler, so it is
     * pinned here.
     */
    @Test
    fun theWidgetDeclaresNoConfigurationActivity() {
        assertEquals(
            "android:configure holds a class name, not a boolean. Anything here and the launcher " +
                "fails to place the widget; see docs/decisions/001-*.",
            null,
            widgetProvider.configure,
        )
    }

    /** One cell is the whole point of the project; see CLAUDE.md §2.5. */
    @Test
    fun theWidgetIsOneCellAndNotResizable() {
        assertEquals(1, widgetProvider.targetCellWidth)
        assertEquals(1, widgetProvider.targetCellHeight)
        assertEquals(
            "resizeMode must stay none, or the user can stretch it into the 2×2 thing every other " +
                "pomodoro app ships",
            AppWidgetProviderInfo.RESIZE_NONE,
            widgetProvider.resizeMode,
        )
    }

    private val widgetProvider: AppWidgetProviderInfo
        get() {
            val provider = AppWidgetManager.getInstance(context)
                .getInstalledProvidersForPackage(context.packageName, null)
                .firstOrNull { it.provider.className.endsWith("PomodoroWidgetProvider") }

            assertNotNull("PomodoroWidgetProvider is not registered", provider)
            return provider!!
        }

    @Test
    fun theBootReceiverIsDeclared() {
        val receivers = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_RECEIVERS)
            .receivers
            ?.map { it.name }
            .orEmpty()

        assertTrue(
            "Without BootReceiver the timer cannot be repaired after a reboot",
            receivers.any { it.endsWith("BootReceiver") },
        )
        assertTrue(receivers.any { it.endsWith("TimerAlarmReceiver") })
        assertTrue(receivers.any { it.endsWith("TimerActionReceiver") })
    }
}
