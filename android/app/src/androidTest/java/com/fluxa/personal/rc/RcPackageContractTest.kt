package com.fluxa.personal.rc

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.hypot

/** Also executed against the non-debuggable release APK, signed only with the local debug QA key. */
class RcPackageContractTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION") // The int PackageManager overload remains required on API 30–32.
    @Test fun identityManifestAndReleaseSurfaceMatchTheRcContract() {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
            PackageManager.GET_PROVIDERS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS
        val info = pm.getPackageInfo(context.packageName, flags)
        val appInfo = requireNotNull(info.applicationInfo)
        assertEquals("Fluxa", pm.getApplicationLabel(appInfo).toString())
        assertEquals("1.0.0", info.versionName)
        assertEquals(3L, info.longVersionCode)
        assertEquals(30, appInfo.minSdkVersion)
        assertEquals(34, appInfo.targetSdkVersion)
        assertEquals(0, appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(0, appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)
        assertFalse(info.requestedPermissions.orEmpty().contains("android.permission.INTERNET"))
        assertTrue(info.providers.orEmpty().all { !it.exported })
        assertTrue(info.services.orEmpty().all { !it.exported })
        assertTrue(info.receivers.orEmpty().all { !it.exported || it.permission == "android.permission.DUMP" })
        val debug = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debug) {
            assertEquals(listOf("com.fluxa.personal.MainActivity"), info.activities.orEmpty().filter { it.exported }.map { it.name })
            for (name in listOf(
                "com.fluxa.personal.ui.SecurityGateTestHook",
                "com.fluxa.personal.ui.feature.backup.BackupPickerTestHook",
                "com.fluxa.personal.ui.designsystem.catalog.FluxaCatalogPreviewsKt",
            )) {
                try {
                    context.classLoader.loadClass(name)
                    fail("Debug-only class was packaged in release: $name")
                } catch (_: ClassNotFoundException) { /* required absence */ }
            }
        }
    }

    @Test fun adaptiveForegroundFitsSafeCircleAndRendersAcrossMasks() {
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue(icon is AdaptiveIconDrawable)
        icon as AdaptiveIconDrawable
        if (Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
        val glyph = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        icon.foreground.setBounds(0, 0, 108, 108)
        icon.foreground.draw(Canvas(glyph))
        var ink = 0
        for (y in 0 until 108) for (x in 0 until 108) {
            if (Color.alpha(glyph.getPixel(x, y)) > 16) {
                ink++
                assertTrue("Ink outside the 66dp safe circle", hypot(x + .5 - 54, y + .5 - 54) <= 33.5)
            }
        }
        assertTrue(ink > 100)
        val sheet = Bitmap.createBitmap(648, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(243, 240, 231))
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23, 63, 54); textSize = 18f }
        val names = listOf("Círculo", "Superelipse", "Arredondado")
        repeat(3) { index ->
            canvas.save()
            canvas.translate(index * 216f + 12, 8f)
            val mask = Path().apply {
                when (index) {
                    0 -> addCircle(96f, 96f, 96f, Path.Direction.CW)
                    1 -> {
                        moveTo(0f, 96f)
                        cubicTo(0f, 8f, 8f, 0f, 96f, 0f)
                        cubicTo(184f, 0f, 192f, 8f, 192f, 96f)
                        cubicTo(192f, 184f, 184f, 192f, 96f, 192f)
                        cubicTo(8f, 192f, 0f, 184f, 0f, 96f)
                        close()
                    }
                    else -> addRoundRect(RectF(0f, 0f, 192f, 192f), 42f, 42f, Path.Direction.CW)
                }
            }
            canvas.clipPath(mask)
            icon.background.setBounds(0, 0, 192, 192)
            icon.foreground.setBounds(0, 0, 192, 192)
            icon.background.draw(canvas)
            icon.foreground.draw(canvas)
            canvas.restore()
            canvas.drawText(names[index], index * 216f + 18, 224f, text)
        }
        val output = File(context.getExternalFilesDir(null), "rc-icon-masks.png")
        output.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
