package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import java.awt.Rectangle

/**
 * Описывает кликабельную область action на конкретном переходе.
 */
data class ActionHitArea(
    val edgeId: String,
    val actionIndex: Int,
    val bounds: Rectangle
)
