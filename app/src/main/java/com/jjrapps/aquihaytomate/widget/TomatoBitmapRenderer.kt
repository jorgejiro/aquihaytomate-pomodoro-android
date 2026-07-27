package com.jjrapps.aquihaytomate.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.render.TomatoGeometry
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Rasterises the tomato for the widget.
 *
 * **The shape comes from [TomatoGeometry], the same pure function the Compose canvas uses.** If the app
 * and the widget each worked out the liquid surface on their own they would drift apart — the same reason
 * `TimerMath` and `SlotPlanner` exist. See ADR 001, point 3.
 *
 * The bitmap has to fit through a Binder transaction, whose Bundle is around 1 MB. At 34 dp on xxxhdpi
 * this is ~136 px square in ARGB_8888, about 74 KB: plenty of room, but not a reason to rasterise larger
 * "just in case".
 */
@Singleton
class TomatoBitmapRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val liquidPath = Path()
    private val clipPath = Path()

    /**
     * @param fillFraction 1 is brim-full, 0 is empty.
     * @param dimmed true while paused: the liquid drops to 45% so a stopped timer reads as stopped even
     *   before the pause glyph is noticed.
     */
    fun render(
        slotType: SlotType,
        fillFraction: Float,
        dimmed: Boolean,
        showCalyx: Boolean,
        sizeDp: Int = DEFAULT_SIZE_DP,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val colors = phaseColorsOf(slotType)
        val size = sizePx.toFloat()
        val diameter = min(size, size)

        clipPath.reset()
        clipPath.addOval(RectF(0f, 0f, diameter, diameter), Path.Direction.CW)

        canvas.withClip(clipPath) {
            // The hollow first, then the liquid over it.
            fillPaint.color = colors.ghost.toArgb()
            drawRect(0f, 0f, size, size, fillPaint)

            if (fillFraction > 0f) {
                fillPaint.color = colors.fill.toArgb()
                if (dimmed) fillPaint.alpha = PAUSED_ALPHA
                buildLiquidPath(size, fillFraction, density)
                drawPath(liquidPath, fillPaint)
                fillPaint.alpha = 255
            }
        }

        if (fillFraction < 1f) {
            strokePaint.color = colors.deep.toArgb()
            strokePaint.strokeWidth = OUTLINE_DP * density
            val inset = strokePaint.strokeWidth / 2f
            canvas.drawOval(
                RectF(inset, inset, diameter - inset, diameter - inset),
                strokePaint,
            )
        }

        // The calyx is the non-chromatic cue for breaks, exactly as in the app: at 40 dp there is no room
        // for a label, so colour alone would be the only signal without it.
        if (showCalyx) drawCalyx(canvas, size, colors.bright.toArgb())

        return bitmap
    }

    /**
     * The surface is drawn flat here, with no wave phase.
     *
     * The widget cannot animate, and a frozen wave just looks like a wonky line. The still surface plus
     * the level is all the information the 40 dp readout carries.
     */
    private fun buildLiquidPath(size: Float, fillFraction: Float, density: Float) {
        liquidPath.reset()
        val points = TomatoGeometry.liquidSurfacePoints(
            width = size,
            height = size,
            fillFraction = fillFraction,
            phaseRad = 0f,
            amplitudePx = 0f,
            stepPx = TomatoGeometry.STEP_DP * density,
        )
        if (points.isEmpty()) return

        liquidPath.moveTo(points[0], points[1])
        var i = 2
        while (i < points.size) {
            liquidPath.lineTo(points[i], points[i + 1])
            i += 2
        }
        liquidPath.lineTo(size, size)
        liquidPath.lineTo(0f, size)
        liquidPath.close()
    }

    private fun drawCalyx(canvas: Canvas, size: Float, color: Int) {
        val width = size * CALYX_WIDTH_FRACTION
        val height = width / 2f
        val centreX = size / 2f
        val baseY = height * 0.55f

        fillPaint.color = color
        val path = Path()
        listOf(-1f, 0f, 1f).forEach { slot ->
            val tipX = centreX + slot * width * 0.36f
            val tipY = baseY - height * if (slot == 0f) 1f else 0.72f
            val halfWidth = width * 0.17f
            path.moveTo(centreX + slot * width * 0.30f - halfWidth, baseY)
            path.quadTo(tipX - halfWidth * 0.6f, tipY, tipX, tipY)
            path.quadTo(
                tipX + halfWidth * 0.6f,
                tipY,
                centreX + slot * width * 0.30f + halfWidth,
                baseY,
            )
            path.close()
        }
        canvas.drawPath(path, fillPaint)
    }

    private companion object {
        const val DEFAULT_SIZE_DP = 34
        const val OUTLINE_DP = 1.6f
        const val CALYX_WIDTH_FRACTION = 0.164f
        const val PAUSED_ALPHA = 115 // ~45%
        const val MIN_SIZE_PX = 24
        const val MAX_SIZE_PX = 256
    }
}
