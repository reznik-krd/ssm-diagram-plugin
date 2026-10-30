package com.reznik.krd.ssmdiagramplugin.ui.diagram.model

import com.reznik.krd.ssmdiagramplugin.model.SsmState

/**
 * Представляет визуальный узел диаграммы и связь с логическим состоянием.
 */
data class DiagramNode(
    val id: String,
    val logicalId: String,
    val label: String,
    val type: Type,
    val state: SsmState? = null,
    val visualCopy: Boolean = false
) {

    /** Допустимые разновидности элемента модели. */
    enum class Type {
        INITIAL_MARKER,
        STATE,
        END_MARKER,
        CHOICE
    }
}