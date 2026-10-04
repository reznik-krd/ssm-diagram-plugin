package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Point

/**
 * Хранит маршрут ребра и позиции подписей event, guard и actions.
 */
data class EdgeLayout(
    val points: List<Point>,

    /**
     * Позиция event.
     */
    val eventLabelPosition: Point? = null,

    /**
     * Позиция guard.
     */
    val guardLabelPosition: Point? = null,

    /**
     * Позиции actions этого transition.
     *
     * Action принадлежит edge,
     * но визуально располагается под target.
     */
    val actionLabelPositions: List<Point> = emptyList()
)