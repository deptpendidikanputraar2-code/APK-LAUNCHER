package com.example.launcher.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WallpaperGlassStyle
import com.example.launcher.model.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

enum class SystemWallpaperTarget(val label: String) {
    HOME("Beranda HP"),
    LOCK("Layar Kunci HP"),
    BOTH("Beranda & Layar Kunci")
}

object WallpaperStorageManager {

    private val bitmapCache = ConcurrentHashMap<String, ImageBitmap>()

    /**
     * Imports a user-selected gallery image URI (from zero-permission Android Photo Picker)
     * into app-private storage so it stays accessible after device restarts without needing storage permissions.
     */
    suspend fun importCustomWallpaper(
        context: Context,
        sourceUri: Uri,
        isLockScreen: Boolean
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaledBitmapFromUri(context, sourceUri, maxDimension = 1800)
                ?: return@withContext null

            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val fileName = if (isLockScreen) {
                "lock_wp_${System.currentTimeMillis()}.jpg"
            } else {
                "home_wp_${System.currentTimeMillis()}.jpg"
            }
            // Clean up older files for the same slot so internal storage stays tiny
            val prefix = if (isLockScreen) "lock_wp_" else "home_wp_"
            dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }

            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            val cacheKey = "${outFile.absolutePath}:${outFile.lastModified()}"
            bitmapCache[cacheKey] = bitmap.asImageBitmap()
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun loadCustomWallpaperBitmap(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        bitmapCache[cacheKey]?.let { return@withContext it }

        runCatching {
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@withContext null
            val imageBitmap = decoded.asImageBitmap()
            if (bitmapCache.size > 6) bitmapCache.clear()
            bitmapCache[cacheKey] = imageBitmap
            imageBitmap
        }.getOrNull()
    }

    /**
     * Renders and applies the current Vitro WallpaperConfig directly to Android's native system WallpaperManager.
     */
    suspend fun applyToAndroidSystemWallpaper(
        context: Context,
        config: WallpaperConfig,
        target: SystemWallpaperTarget
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            val bitmap = buildSystemWallpaperBitmap(context, config)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    SystemWallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    SystemWallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    SystemWallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wm.setBitmap(bitmap, null, true, flags)
            } else {
                wm.setBitmap(bitmap)
            }
            "Wallpaper berhasil diterapkan ke ${target.label}"
        }
    }

    private fun decodeScaledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val largest = maxOf(w, h)
                    if (largest > maxDimension && largest > 0) {
                        val scale = maxDimension.toFloat() / largest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
        }

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val largest = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sample = 1
        while (largest / sample > maxDimension) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun buildSystemWallpaperBitmap(context: Context, config: WallpaperConfig): Bitmap {
        val base: Bitmap = when (config.preset) {
            WallpaperPreset.CUSTOM_PHOTO -> {
                val file = File(config.customImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.copy(Bitmap.Config.ARGB_8888, true)
                } else null
            }
            WallpaperPreset.AURORA -> BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_wallpaper_aurora_1795471682249
            )?.copy(Bitmap.Config.ARGB_8888, true)
            WallpaperPreset.PRISM -> BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_wallpaper_prism_1795471682679
            )?.copy(Bitmap.Config.ARGB_8888, true)
            WallpaperPreset.DUSK -> BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_wallpaper_dusk_1795471681919
            )?.copy(Bitmap.Config.ARGB_8888, true)
            else -> null
        } ?: renderProceduralBitmap(config.preset)

        // Bake color tone, fluted glass ribs, and subtle dim scrim into exported system wallpaper
        val canvas = Canvas(base)
        val w = base.width.toFloat()
        val h = base.height.toFloat()

        if (config.colorTone.tint.alpha > 0.01f) {
            val tonePaint = Paint().apply {
                color = config.colorTone.tint.copy(alpha = 0.24f).toArgb()
            }
            canvas.drawRect(0f, 0f, w, h, tonePaint)
        }

        if (config.glassStyle == WallpaperGlassStyle.FLUTED_REEDED && config.glassIntensity > 0.05f) {
            val ribCount = 26
            val ribW = w / ribCount
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (i in 0 until ribCount) {
                val x0 = i * ribW
                paint.shader = LinearGradient(
                    x0, 0f, x0 + ribW, 0f,
                    intArrayOf(
                        android.graphics.Color.argb((70 * config.glassIntensity).toInt(), 255, 255, 255),
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.argb((80 * config.glassIntensity).toInt(), 4, 10, 28)
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(x0, 0f, x0 + ribW, h, paint)
            }
        } else if (config.glassStyle == WallpaperGlassStyle.FROSTED_GLASS && config.glassIntensity > 0.05f) {
            val frostPaint = Paint().apply {
                color = android.graphics.Color.argb((55 * config.glassIntensity).toInt(), 225, 240, 255)
            }
            canvas.drawRect(0f, 0f, w, h, frostPaint)
        }

        if (config.dimAlpha > 0.02f) {
            val dimPaint = Paint().apply {
                color = android.graphics.Color.argb((config.dimAlpha * 255).toInt().coerceIn(0, 190), 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, w, h, dimPaint)
        }
        return base
    }

    private fun renderProceduralBitmap(preset: WallpaperPreset): Bitmap {
        val w = 1080
        val h = 2340
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colors = when (preset) {
            WallpaperPreset.LIVE_AURORA_WAVE -> intArrayOf(0xFF04091A.toInt(), 0xFF0B2447.toInt(), 0xFF0EA5E9.toInt(), 0xFF1E1B4B.toInt())
            WallpaperPreset.LIVE_NEBULA_ORBS -> intArrayOf(0xFF050414.toInt(), 0xFF1E1148.toInt(), 0xFF4C1D95.toInt(), 0xFF0F172A.toInt())
            WallpaperPreset.LIVE_LIQUID_LAVA -> intArrayOf(0xFF1A0714.toInt(), 0xFF7C2D12.toInt(), 0xFFDB2777.toInt(), 0xFF090B1A.toInt())
            WallpaperPreset.LIVE_CYBER_GRID -> intArrayOf(0xFF06041A.toInt(), 0xFF2E1065.toInt(), 0xFF0891B2.toInt(), 0xFF020617.toInt())
            WallpaperPreset.LIVE_RAIN_GLASS -> intArrayOf(0xFF07111F.toInt(), 0xFF0F2942.toInt(), 0xFF1E3A5F.toInt(), 0xFF040914.toInt())
            WallpaperPreset.LIVE_SOLAR_DYNAMIC -> intArrayOf(0xFF0F172A.toInt(), 0xFF1D4ED8.toInt(), 0xFF38BDF8.toInt(), 0xFF0B1120.toInt())
            WallpaperPreset.GLASS_REFRACTION -> intArrayOf(0xFF07142B.toInt(), 0xFF0EA5E9.toInt(), 0xFF818CF8.toInt(), 0xFF091224.toInt())
            WallpaperPreset.GLASS_PRISM_BLOOM -> intArrayOf(0xFF090E23.toInt(), 0xFF312E81.toInt(), 0xFF06B6D4.toInt(), 0xFF0B1120.toInt())
            else -> intArrayOf(0xFF050914.toInt(), 0xFF101D3A.toInt(), 0xFF1E293B.toInt(), 0xFF040711.toInt())
        }
        paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        orbPaint.shader = RadialGradient(
            w * 0.25f, h * 0.28f, w * 0.65f,
            intArrayOf(0x8838BDF8.toInt(), 0x0038BDF8),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(w * 0.25f, h * 0.28f, w * 0.65f, orbPaint)

        orbPaint.shader = RadialGradient(
            w * 0.78f, h * 0.65f, w * 0.6f,
            intArrayOf(0x77A855F7.toInt(), 0x00A855F7),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(w * 0.78f, h * 0.65f, w * 0.6f, orbPaint)

        return bmp
    }
}
