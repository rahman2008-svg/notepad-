package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class NoteColorTheme(
    val name: String,
    val lightColor: Color,
    val darkColor: Color,
    val onLightColor: Color = Color(0xFF1C1B1F),
    val onDarkColor: Color = Color(0xFFE6E1E5)
)

val NoteColorsList = listOf(
    NoteColorTheme("Default", Color(0xFFFBFDF7), Color(0xFF12140F)),
    NoteColorTheme("Coral", Color(0xFFFFDAD4), Color(0xFF410002)),
    NoteColorTheme("Peach", Color(0xFFFFDDAE), Color(0xFF331B00)),
    NoteColorTheme("Sand", Color(0xFFFFF9BE), Color(0xFF231D00)),
    NoteColorTheme("Mint", Color(0xFFD1E8D5), Color(0xFF0F2C1D)),
    NoteColorTheme("Sage", Color(0xFFE4EBF0), Color(0xFF142733)),
    NoteColorTheme("Ocean", Color(0xFFD6F2F7), Color(0xFF0D2533)),
    NoteColorTheme("Sky", Color(0xFFD3E4FF), Color(0xFF001B3F)),
    NoteColorTheme("Lavender", Color(0xFFF3E2FD), Color(0xFF280B3F)),
    NoteColorTheme("Blossom", Color(0xFFFFD8E4), Color(0xFF31101D)),
    NoteColorTheme("Rust", Color(0xFFF7E2CE), Color(0xFF2D1600)),
    NoteColorTheme("Slate", Color(0xFFE1E2EC), Color(0xFF1F2432))
)

fun getNoteColor(colorIndex: Int, isDark: Boolean): Color {
    val index = if (colorIndex in NoteColorsList.indices) colorIndex else 0
    return if (isDark) NoteColorsList[index].darkColor else NoteColorsList[index].lightColor
}

fun getNoteOnColor(colorIndex: Int, isDark: Boolean): Color {
    val index = if (colorIndex in NoteColorsList.indices) colorIndex else 0
    return if (isDark) NoteColorsList[index].onDarkColor else NoteColorsList[index].onLightColor
}
