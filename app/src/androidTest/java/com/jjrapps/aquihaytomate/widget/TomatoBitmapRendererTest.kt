package com.jjrapps.aquihaytomate.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The widget's action glyph, pinned at the pixel level.
 *
 * It used to be the character `▸` in a `TextView` asking for Space Grotesk, which has no such glyph: the
 * fallback painted a 13 sp speck that users read as a smudge on a full tomato, not as a play button. Now it
 * is a `Path` on the bitmap, and these tests exist so it cannot quietly go missing or lose its contrast
 * again — neither of which the compiler can see.
 */
@RunWith(AndroidJUnit4::class)
class TomatoBitmapRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val renderer = TomatoBitmapRenderer(context)
    private val bone = TextPrimary.toArgb()

    private fun render(glyph: WidgetGlyph, large: Boolean, fillFraction: Float = 1f): Bitmap =
        renderer.render(
            slotType = SlotType.FOCUS,
            fillFraction = fillFraction,
            dimmed = false,
            showCalyx = false,
            glyph = glyph,
            glyphLarge = large,
        )

    /** A stopped timer is all glyph: the very centre of the tomato has to be the play triangle. */
    @Test
    fun theLargePlayGlyphCoversTheCentre() {
        val bitmap = render(WidgetGlyph.PLAY, large = true)

        assertEquals(bone, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }

    @Test
    fun noGlyphLeavesTheCentreToTheLiquid() {
        val bitmap = render(WidgetGlyph.NONE, large = true)

        assertFalse(
            "The tomato must not paint bone pixels of its own",
            bitmap.containsColour(bone),
        )
    }

    /** The small glyph shares the plate with the figure, so it lives in the bottom fifth. */
    @Test
    fun theSmallPauseGlyphSitsBelowTheFigure() {
        val bitmap = render(WidgetGlyph.PAUSE, large = false)

        assertTrue(
            "No pause glyph in the lower part of the tomato",
            bitmap.containsColour(bone, fromY = (bitmap.height * 0.7f).toInt()),
        )
        assertFalse(
            "The small glyph must stay clear of the digits above it",
            bitmap.containsColour(bone, toY = (bitmap.height * 0.6f).toInt()),
        )
    }

    /** Over an almost empty tomato the glyph is on the near-black hollow, and must still be there. */
    @Test
    fun theGlyphSurvivesAnEmptyTomato() {
        val bitmap = render(WidgetGlyph.PLAY, large = true, fillFraction = 0.02f)

        assertEquals(bone, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }

    private fun Bitmap.containsColour(colour: Int, fromY: Int = 0, toY: Int = height): Boolean {
        for (y in fromY until toY) {
            for (x in 0 until width) {
                if (getPixel(x, y) == colour) return true
            }
        }
        return false
    }
}
