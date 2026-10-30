package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

/** Параметры масштаба, отступов и точности взаимодействия с диаграммой. */
internal object DiagramInteractionConstants {
    const val DEFAULT_ZOOM = 1.0
    const val MIN_ZOOM = 0.25
    const val MAX_ZOOM = 2.5
    const val ZOOM_STEP = 0.10
    const val FIT_PADDING = 40
    /** Допуск попадания по ребру в экранных пикселях. */
    const val EDGE_CLICK_TOLERANCE = 10.0
}
