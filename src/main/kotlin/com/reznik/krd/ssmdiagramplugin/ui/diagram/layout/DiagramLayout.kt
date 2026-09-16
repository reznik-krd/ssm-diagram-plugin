package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Dimension

data class DiagramLayout(
    val nodes: Map<String, NodeLayout>,
    val edges: Map<String, EdgeLayout>,
    val size: Dimension
)