package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AquiHayTomateShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // chart bars
    small = RoundedCornerShape(8.dp),         // chips, heatmap cells
    medium = RoundedCornerShape(12.dp),       // settings groups
    large = RoundedCornerShape(18.dp),        // bottom sheets
    extraLarge = RoundedCornerShape(50),
)
