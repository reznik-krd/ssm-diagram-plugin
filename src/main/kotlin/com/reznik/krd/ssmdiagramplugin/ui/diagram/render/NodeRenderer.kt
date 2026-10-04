package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point

/**
 * Renders state, choice, initial and end nodes.
 */
internal class NodeRenderer(
    private val interaction: DiagramInteractionState
) {

    fun render(
        g2: Graphics2D,
        graph: DiagramGraph,
        layout: DiagramLayout
    ) {

        /*
         * Concrete edge selected by clicking a transition.
         */
        val selectedEdge =
            interaction.selectedEdgeId
                ?.let { selectedEdgeId ->
                    graph.edges
                        .firstOrNull {
                            it.id == selectedEdgeId
                        }
                }

        /*
         * Logical state selected by clicking a state.
         *
         * logicalId is used because ReadableGraphTransformer
         * may create several visual copies of one logical state.
         */
        val selectedNodeLogicalId =
            interaction.selectedNodeLogicalId

        graph.nodes.forEach { node ->

            val nodeLayout =
                layout.nodes[node.id]
                    ?: return@forEach

            val position =
                Point(
                    nodeLayout.x,
                    nodeLayout.y
                )

            /*
             * State was explicitly selected.
             */
            val selectedByNode =
                selectedNodeLogicalId != null &&
                        node.logicalId == selectedNodeLogicalId

            /*
             * Node is source/target of explicitly selected edge.
             */
            val selectedByEdge =
                selectedEdge != null &&
                        (
                                node.id == selectedEdge.source ||
                                        node.id == selectedEdge.target
                                )

            val selected =
                selectedByNode ||
                        selectedByEdge

            when (node.type) {

                DiagramNode.Type.INITIAL_MARKER ->
                    drawInitialMarker(
                        g2 = g2,
                        position = position,
                        width = nodeLayout.width,
                        height = nodeLayout.height,
                        selected = selected
                    )

                DiagramNode.Type.END_MARKER ->
                    drawEndMarker(
                        g2 = g2,
                        position = position,
                        width = nodeLayout.width,
                        height = nodeLayout.height,
                        selected = selected
                    )

                DiagramNode.Type.CHOICE ->
                    drawChoiceNode(
                        g2 = g2,
                        node = node,
                        position = position,
                        width = nodeLayout.width,
                        height = nodeLayout.height,
                        selected = selected
                    )

                DiagramNode.Type.STATE ->
                    drawStateNode(
                        g2 = g2,
                        node = node,
                        position = position,
                        width = nodeLayout.width,
                        height = nodeLayout.height,
                        selected = selected
                    )
            }
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private fun drawStateNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {

        val hovered =
            node.id == interaction.hoveredNodeId

        g2.color =
            when {

                selected ->
                    SELECTED_BACKGROUND

                hovered ->
                    HOVER_BACKGROUND

                else ->
                    JBColor.PanelBackground
            }

        g2.fillRoundRect(
            position.x,
            position.y,
            width,
            height,
            14,
            14
        )

        g2.color =
            when {

                selected ->
                    SELECTED_COLOR

                hovered ->
                    JBColor.foreground()

                else ->
                    JBColor.GRAY
            }

        g2.stroke =
            BasicStroke(
                when {

                    selected ->
                        SELECTED_NODE_WIDTH

                    hovered ->
                        HOVER_NODE_WIDTH

                    else ->
                        NORMAL_NODE_WIDTH
                }
            )

        g2.drawRoundRect(
            position.x,
            position.y,
            width,
            height,
            14,
            14
        )

        val oldFont =
            g2.font

        g2.font =
            oldFont.deriveFont(
                Font.PLAIN,
                13f
            )

        val metrics =
            g2.fontMetrics

        val textX =
            position.x +
                    (
                            width -
                                    metrics.stringWidth(node.label)
                            ) / 2

        val textY =
            position.y +
                    (
                            height -
                                    metrics.height
                            ) / 2 +
                    metrics.ascent

        g2.color =
            if (selected) {
                SELECTED_TEXT_COLOR
            } else {
                JBColor.foreground()
            }

        g2.drawString(
            node.label,
            textX,
            textY
        )

        g2.font =
            oldFont
    }

    // -------------------------------------------------------------------------
    // Choice
    // -------------------------------------------------------------------------

    private fun drawChoiceNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val halfSize =
            26

        val xPoints =
            intArrayOf(
                centerX,
                centerX + halfSize,
                centerX,
                centerX - halfSize
            )

        val yPoints =
            intArrayOf(
                centerY - halfSize,
                centerY,
                centerY + halfSize,
                centerY
            )

        val hovered =
            node.id == interaction.hoveredNodeId

        g2.color =
            when {

                selected ->
                    SELECTED_BACKGROUND

                hovered ->
                    HOVER_BACKGROUND

                else ->
                    JBColor.PanelBackground
            }

        g2.fillPolygon(
            xPoints,
            yPoints,
            4
        )

        g2.color =
            when {

                selected ->
                    SELECTED_COLOR

                hovered ->
                    JBColor.foreground()

                else ->
                    JBColor.GRAY
            }

        g2.stroke =
            BasicStroke(
                when {

                    selected ->
                        SELECTED_NODE_WIDTH

                    hovered ->
                        HOVER_NODE_WIDTH

                    else ->
                        NORMAL_NODE_WIDTH
                }
            )

        g2.drawPolygon(
            xPoints,
            yPoints,
            4
        )

        g2.color =
            if (selected) {
                SELECTED_TEXT_COLOR
            } else {
                JBColor.foreground()
            }

        g2.drawString(
            node.label,
            centerX +
                    halfSize +
                    12,
            centerY + 5
        )
    }

    // -------------------------------------------------------------------------
    // Initial
    // -------------------------------------------------------------------------

    private fun drawInitialMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val radius =
            if (selected) {
                10
            } else {
                8
            }

        g2.color =
            if (selected) {
                SELECTED_COLOR
            } else {
                JBColor.foreground()
            }

        g2.fillOval(
            centerX - radius,
            centerY - radius,
            radius * 2,
            radius * 2
        )
    }

    // -------------------------------------------------------------------------
    // End
    // -------------------------------------------------------------------------

    private fun drawEndMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val outerRadius =
            if (selected) {
                12
            } else {
                10
            }

        val innerRadius =
            if (selected) {
                6
            } else {
                5
            }

        g2.color =
            if (selected) {
                SELECTED_COLOR
            } else {
                JBColor.foreground()
            }

        g2.stroke =
            BasicStroke(
                if (selected) {
                    SELECTED_NODE_WIDTH
                } else {
                    NORMAL_NODE_WIDTH
                }
            )

        g2.drawOval(
            centerX - outerRadius,
            centerY - outerRadius,
            outerRadius * 2,
            outerRadius * 2
        )

        g2.fillOval(
            centerX - innerRadius,
            centerY - innerRadius,
            innerRadius * 2,
            innerRadius * 2
        )
    }

    // -------------------------------------------------------------------------
    // Style
    // -------------------------------------------------------------------------

    private companion object {

        const val NORMAL_NODE_WIDTH =
            1.4f

        const val HOVER_NODE_WIDTH =
            2.2f

        const val SELECTED_NODE_WIDTH =
            3.0f

        val SELECTED_COLOR =
            JBColor(
                Color(45, 125, 220),
                Color(90, 165, 255)
            )

        val SELECTED_BACKGROUND =
            JBColor(
                Color(225, 238, 252),
                Color(48, 65, 82)
            )

        val SELECTED_TEXT_COLOR =
            JBColor(
                Color(25, 80, 150),
                Color(150, 205, 255)
            )

        val HOVER_BACKGROUND =
            JBColor(
                Color(235, 242, 252),
                Color(55, 65, 78)
            )
    }
}