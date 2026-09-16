package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Point

data class EdgeLayout(
    val points: List<Point>,

    /**
     * Позиция event.
     *
     * Например:
     * CREATED -- CHECK --> CHECKING
     */
    val eventLabelPosition: Point? = null,

    /**
     * Позиция guard.
     *
     * Guard стараемся располагать ближе
     * к target конкретного transition.
     */
    val guardLabelPosition: Point? = null
)