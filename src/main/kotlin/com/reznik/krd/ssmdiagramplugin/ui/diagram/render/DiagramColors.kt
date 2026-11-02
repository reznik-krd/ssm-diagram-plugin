package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.intellij.ui.JBColor
import java.awt.Color

/** Цветовая палитра диаграммы для светлой и тёмной тем IntelliJ IDEA. */
internal object DiagramColors {

    // State — Green
    val STATE_BACKGROUND = JBColor(
        Color(0xE4F4E9),
        Color(0x193D34)
    )

    val STATE_BORDER = JBColor(
        Color(0x50A875),
        Color(0x42AE87)
    )

    val STATE_TEXT = JBColor(
        Color(0x256C45),
        Color(0x9BE5C3)
    )

    // Choice — Lava Orange
    val CHOICE_BACKGROUND = JBColor(
        Color(0xFFE6D5),
        Color(0x4A2A1F)
    )

    val CHOICE_BORDER = JBColor(
        Color(0xE67E22),
        Color(0xFF9A3C)
    )

    val CHOICE_TEXT = JBColor(
        Color(0x8A4312),
        Color(0xFFD2A6)
    )

    // Selection — Blue
    val SELECTED_BORDER = JBColor(
        Color(0x2383E2),
        Color(0x66B2FF)
    )

    val SELECTED_TEXT = JBColor(
        Color(0x1765AD),
        Color(0xA4D3FF)
    )

    val SELECTED_LABEL_BACKGROUND = JBColor(
        Color(0xE1EEFC),
        Color(0x304152)
    )

    // Hover
    val STATE_HOVER_BORDER = JBColor(
        Color(0x349766),
        Color(0x70D6A5)
    )

    val CHOICE_HOVER_BORDER = JBColor(
        Color(0xC96A16),
        Color(0xFFB16B)
    )

    // Transitions
    val EDGE = JBColor(
        Color(0x8291A6),
        Color(0x879AB3)
    )

    // Selected state — Blue
    val SELECTED_STATE_BACKGROUND = JBColor(
        Color(0xDBEDFF),
        Color(0x24445A)
    )

    val SELECTED_STATE_TEXT = JBColor(
        Color(0x14558A),
        Color(0xC7E3FF)
    )

    // Incoming source — Green
    val INCOMING_STATE_BACKGROUND = JBColor(
        Color(0xDCF5E8),
        Color(0x2B4438)
    )

    val INCOMING_STATE_BORDER = JBColor(
        Color(0x39A66D),
        Color(0x52C78C)
    )

    val INCOMING_STATE_TEXT = JBColor(
        Color(0x216943),
        Color(0xAEF3C9)
    )

    // Focus mode
    const val UNFOCUSED_OPACITY = 0.35f
    const val FULL_OPACITY = 1.0f
    const val INCOMING_NODE_BORDER_WIDTH = 2.2f

}