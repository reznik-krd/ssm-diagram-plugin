package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Dimension

/**
 * Хранит рассчитанную геометрию узлов, рёбер и общий размер диаграммы.
 */
data class DiagramLayout(
    val nodes: Map<String, NodeLayout>,
    val edges: Map<String, EdgeLayout>,
    val size: Dimension
)