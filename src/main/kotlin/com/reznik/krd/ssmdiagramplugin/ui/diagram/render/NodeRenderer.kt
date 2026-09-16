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

/** Renders state, choice, initial and end nodes. */
internal class NodeRenderer(
    private val interaction: DiagramInteractionState
) {

    fun render(g2: Graphics2D, graph: DiagramGraph, layout: DiagramLayout) {
        drawNodes(g2, graph, layout)
    }

    private fun drawNodes(
        g2: Graphics2D,
        graph: DiagramGraph,
        layout: DiagramLayout
    ) {

        graph.nodes.forEach { node ->

            val nodeLayout =
                layout.nodes[node.id]
                    ?: return@forEach

            val position =
                Point(
                    nodeLayout.x,
                    nodeLayout.y
                )

            when (node.type) {

                DiagramNode.Type.INITIAL_MARKER ->
                    drawInitialMarker(
                        g2,
                        position,
                        nodeLayout.width,
                        nodeLayout.height
                    )

                DiagramNode.Type.END_MARKER ->
                    drawEndMarker(
                        g2,
                        position,
                        nodeLayout.width,
                        nodeLayout.height
                    )

                DiagramNode.Type.CHOICE ->
                    drawChoiceNode(
                        g2,
                        node,
                        position,
                        nodeLayout.width,
                        nodeLayout.height
                    )

                DiagramNode.Type.STATE ->
                    drawStateNode(
                        g2,
                        node,
                        position,
                        nodeLayout.width,
                        nodeLayout.height
                    )
            }
        }
    }

    private fun drawStateNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int
    ) {

        val hovered =
            node.id == interaction.hoveredNodeId

        g2.color =
            if (hovered) {
                JBColor(
                    Color(235, 242, 252),
                    Color(55, 65, 78)
                )
            } else {
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
            if (hovered) {
                JBColor.foreground()
            } else {
                JBColor.GRAY
            }

        g2.stroke =
            BasicStroke(
                if (hovered) {
                    2.2f
                } else {
                    1.4f
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
                                    metrics.stringWidth(
                                        node.label
                                    )
                            ) / 2

        val textY =
            position.y +
                    (
                            height -
                                    metrics.height
                            ) / 2 +
                    metrics.ascent

        g2.color =
            JBColor.foreground()

        g2.drawString(
            node.label,
            textX,
            textY
        )

        g2.font =
            oldFont
    }

    private fun drawChoiceNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val halfSize = 26

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
            if (hovered) {
                JBColor(
                    Color(235, 242, 252),
                    Color(55, 65, 78)
                )
            } else {
                JBColor.PanelBackground
            }

        g2.fillPolygon(
            xPoints,
            yPoints,
            4
        )

        g2.color =
            if (hovered) {
                JBColor.foreground()
            } else {
                JBColor.GRAY
            }

        g2.stroke =
            BasicStroke(
                if (hovered) {
                    2.2f
                } else {
                    1.4f
                }
            )

        g2.drawPolygon(
            xPoints,
            yPoints,
            4
        )

        /*
         * Название choice справа от diamond.
         */
        g2.color =
            JBColor.foreground()

        g2.drawString(
            node.label,
            centerX + halfSize + 12,
            centerY + 5
        )
    }

    private fun drawInitialMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val radius = 8

        g2.color =
            JBColor.foreground()

        g2.fillOval(
            centerX - radius,
            centerY - radius,
            radius * 2,
            radius * 2
        )
    }

    private fun drawEndMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int
    ) {

        val centerX =
            position.x +
                    width / 2

        val centerY =
            position.y +
                    height / 2

        val outerRadius = 10
        val innerRadius = 5

        g2.color =
            JBColor.foreground()

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
    // Arrow
    // -------------------------------------------------------------------------

}
