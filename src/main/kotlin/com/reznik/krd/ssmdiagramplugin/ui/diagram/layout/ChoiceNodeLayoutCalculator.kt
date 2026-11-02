package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Dimension
import java.awt.Font
import java.awt.font.FontRenderContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/** Рассчитывает текст и геометрию ромба Choice без обрезки технического имени. */
internal object ChoiceNodeLayoutCalculator {
    const val FONT_SIZE = 11
    const val LINE_HEIGHT = 15
    private const val MIN_WIDTH = 150
    private const val MIN_HEIGHT = 90
    private const val MAX_LINE_LENGTH = 16
    private const val PADDING_X = 17
    private const val PADDING_Y = 9

    val font: Font = Font(Font.DIALOG, Font.PLAIN, FONT_SIZE)
    private val context = FontRenderContext(null, true, true)

    /** Переносит длинное имя по границам подчёркиваний, не теряя символов. */
    fun lines(label: String): List<String> {
        if (label.length <= MAX_LINE_LENGTH) return listOf(label)
        val result = mutableListOf<String>()
        var line = ""
        label.split("_").forEachIndexed { index, part ->
            val token = if (index == 0) part else "_$part"
            if (line.isNotEmpty() && line.length + token.length > MAX_LINE_LENGTH) {
                result += line
                line = ""
            }
            // Слишком длинные фрагменты разделяем без потери символов.
            var remaining = token
            while (remaining.length > MAX_LINE_LENGTH) {
                val available = MAX_LINE_LENGTH - line.length
                if (available == 0) { result += line; line = ""; continue }
                line += remaining.take(available)
                remaining = remaining.drop(available)
                result += line
                line = ""
            }
            line += remaining
        }
        if (line.isNotEmpty()) result += line
        return result.ifEmpty { listOf(label) }
    }

    /** Подбирает размеры с учётом сужения ромба у вершин. */
    fun calculate(label: String): Dimension {
        val parts = lines(label)
        val height = max(MIN_HEIGHT, parts.size * LINE_HEIGHT + 2 * PADDING_Y + 22)
        val firstCenter = (height - (parts.size - 1) * LINE_HEIGHT) / 2.0
        var width = MIN_WIDTH.toDouble()
        parts.forEachIndexed { index, part ->
            val centerY = firstCenter + index * LINE_HEIGHT
            val factor = (1.0 - 2.0 * abs(centerY - height / 2.0) / height).coerceAtLeast(0.25)
            val textWidth = font.getStringBounds(part, context).width
            width = max(width, (textWidth + PADDING_X * 2) / factor)
        }
        return Dimension(ceil(width).toInt(), height)
    }
}
