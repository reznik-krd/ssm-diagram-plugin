package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Point

/**
 * Polyline and label positions calculated for one diagram edge.
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