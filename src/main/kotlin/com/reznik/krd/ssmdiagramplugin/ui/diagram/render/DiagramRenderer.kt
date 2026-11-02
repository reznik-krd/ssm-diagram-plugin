
package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramFocusResolver
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import java.awt.Graphics2D

/**
 * Координирует отрисовку диаграммы и подсветку
 * выбранного состояния с его входящими связями.
 */
class DiagramRenderer(
    private val interaction: DiagramInteractionState,
    private val hitRegistry: DiagramHitRegistry
) {
    private val focusResolver = DiagramFocusResolver()
    private val edgeRenderer = EdgeRenderer(interaction, hitRegistry)
    private val nodeRenderer = NodeRenderer(interaction)

    /**
     * Рассчитывает фокус и отрисовывает переходы,
     * а затем состояния поверх переходов.
     */
    fun render(
        g2: Graphics2D,
        graph: DiagramGraph,
        layout: DiagramLayout
    ) {
        val focus = focusResolver.resolve(
            graph = graph,
            selectedLogicalId = interaction.selectedNodeLogicalId
        )

        hitRegistry.clear()

        edgeRenderer.render(g2, graph, layout, focus)
        nodeRenderer.render(g2, graph, layout, focus)
    }
}
