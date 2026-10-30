package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import java.awt.Rectangle

/**
 * Хранит области интерактивных элементов диаграммы для обработки кликов и hover.
 */
class DiagramHitRegistry {
    val eventAreas = mutableMapOf<String, Rectangle>()
    val guardAreas = mutableMapOf<String, Rectangle>()
    val actionAreas = mutableListOf<ActionHitArea>()

    /**
     * Сбрасывает текущее состояние объекта.
     */
    fun clear() {
        eventAreas.clear()
        guardAreas.clear()
        actionAreas.clear()
    }
}
