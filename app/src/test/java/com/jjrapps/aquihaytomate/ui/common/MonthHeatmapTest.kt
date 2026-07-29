package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The height the monthly map reserves for its grid.
 *
 * This exists because of a real bug: the grid was drawn by splitting the available width across seven
 * columns, but the space reserved for it used a fixed 40 dp cell. On a narrow phone the mismatch was
 * three dp and nobody noticed; on a 800 dp tablet the grid overflowed by 317 dp and buried the legend,
 * the streaks and the sparkline. The arithmetic here is the same one `drawGrid` uses, so the two cannot
 * drift apart again.
 */
class MonthHeatmapTest {

    @Test
    fun `the grid is as tall as the cells the width actually yields`() {
        // 371 dp wide, seven columns, six gaps of 6 dp → cells of 47.86 dp.
        val cell = (371f - 6 * 6) / 7
        assertEquals(cell * 5 + 6 * 4, gridHeight(371.dp, rows = 5).value, 0.01f)
    }

    @Test
    fun `a wider screen means a taller grid, which is the whole point`() {
        val telefono = gridHeight(371.dp, rows = 5)
        val tablet = gridHeight(760.dp, rows = 5)
        assertTrue(
            "A tablet must reserve more height than a phone, or the grid overflows the legend",
            tablet > telefono * 1.9f,
        )
    }

    @Test
    fun `cells stay square`() {
        val filas = 4
        val alto = gridHeight(300.dp, rows = filas)
        val celda = (alto - 6.dp * (filas - 1)) / filas
        val celdaEsperada = (300.dp - 6.dp * 6) / 7
        assertEquals(celdaEsperada.value, celda.value, 0.01f)
    }

    @Test
    fun `no rows means no height, and a single row has no gaps`() {
        assertEquals(0f, gridHeight(371.dp, rows = 0).value, 0f)
        assertEquals(0f, gridHeight(0.dp, rows = 5).value, 0f)
        assertEquals(
            ((371f - 6 * 6) / 7),
            gridHeight(371.dp, rows = 1).value,
            0.01f,
        )
    }
}
