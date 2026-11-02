package com.reznik.krd.ssmdiagramplugin.ui.diagram

/**
 * Настройки видимости подписей на диаграмме.
 *
 * Не изменяют логическую модель StateMachine.
 */
data class DiagramDisplayOptions(
    val hideGuards: Boolean = false,
    val hideActions: Boolean = false
)
