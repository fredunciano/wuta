package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class BeverageType(
    val displayName: String,
    val hydrationFactor: Float, // Hydration efficiency multiplier (e.g. 1.0 = 100%)
    val defaultColor: Color,
    val icon: ImageVector,
    val description: String
) {
    WATER(
        displayName = "Water",
        hydrationFactor = 1.0f,
        defaultColor = Color(0xFF0288D1),
        icon = Icons.Default.Opacity,
        description = "Pure hydration & electrolyte balance"
    ),
    ELECTROLYTE(
        displayName = "Electrolyte / Lemon",
        hydrationFactor = 1.05f,
        defaultColor = Color(0xFF00ACC1),
        icon = Icons.Default.Star,
        description = "Fast cellular absorption"
    ),
    TEA(
        displayName = "Herbal Tea",
        hydrationFactor = 0.95f,
        defaultColor = Color(0xFF43A047),
        icon = Icons.Default.Spa,
        description = "Calming antioxidant infusion"
    ),
    COFFEE(
        displayName = "Coffee",
        hydrationFactor = 0.80f,
        defaultColor = Color(0xFF8D6E63),
        icon = Icons.Default.Coffee,
        description = "Mild diuretic, 80% effective hydration"
    ),
    JUICE(
        displayName = "Fresh Juice",
        hydrationFactor = 0.85f,
        defaultColor = Color(0xFFFB8C00),
        icon = Icons.Default.LocalDrink,
        description = "Vitamins and natural sugars"
    ),
    COCONUT_WATER(
        displayName = "Coconut Water",
        hydrationFactor = 1.0f,
        defaultColor = Color(0xFF26A69A),
        icon = Icons.Default.Eco,
        description = "Rich in potassium and minerals"
    );

    companion object {
        fun fromName(name: String?): BeverageType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: WATER
        }
    }
}
