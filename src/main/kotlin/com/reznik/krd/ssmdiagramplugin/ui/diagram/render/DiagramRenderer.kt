package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import java.awt.Graphics2D

/**
 * Coordinates diagram rendering without performing analysis, layout or navigation.
 */
class DiagramRenderer(
    interaction: DiagramInteractionState,
    private val hitRegistry: DiagramHitRegistry
) {
    private val edgeRenderer = EdgeRenderer(interaction, hitRegistry)
    private val nodeRenderer = NodeRenderer(interaction)

    /** Draws edges first and nodes second so state blocks stay above transition lines. */
    fun render(g2: Graphics2D, graph: DiagramGraph, layout: DiagramLayout) {
        hitRegistry.clear()
        edgeRenderer.render(g2, graph, layout)
        nodeRenderer.render(g2, graph, layout)
    }
}
