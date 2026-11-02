package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramFocus
import java.awt.AlphaComposite

/**
 * Отрисовывает состояния, choice и технические initial/end маркеры.
 */
internal class NodeRenderer(
    private val interaction: DiagramInteractionState
) {


    /**
     * Отрисовывает состояния с учётом выбранного узла,
     * непосредственных предшественников и приглушения остальных.
     */
    fun render(
        g2: Graphics2D,
        graph: DiagramGraph,
        layout: DiagramLayout,
        focus: DiagramFocus?
    ) {
        val selectedEdge = interaction.selectedEdgeId
            ?.let { selectedId ->
                graph.edges.firstOrNull { it.id == selectedId }
            }

        graph.nodes.forEach { node ->
            val nodeLayout = layout.nodes[node.id] ?: return@forEach
            val position = Point(nodeLayout.x, nodeLayout.y)

            val selectedByNode =
                focus?.selectedLogicalId == node.logicalId

            val selectedByEdge = selectedEdge != null &&
                    (node.id == selectedEdge.source ||
                            node.id == selectedEdge.target)

            val selected = selectedByNode || selectedByEdge

            val incomingSource = focus != null &&
                    !selectedByNode &&
                    focus.isIncomingSource(node.logicalId)

            val muted = focus != null &&
                    !focus.isFocusedNode(node.logicalId)

            // Отдельный Graphics2D, чтобы прозрачность не
            // влияла на соседние элементы и другие рендереры.
            val graphics = g2.create() as Graphics2D

            try {
                if (muted) {
                    graphics.composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        DiagramColors.UNFOCUSED_OPACITY
                    )
                }

                when (node.type) {
                    DiagramNode.Type.INITIAL_MARKER ->
                        drawInitialMarker(
                            graphics,
                            position,
                            nodeLayout.width,
                            nodeLayout.height,
                            selected
                        )

                    DiagramNode.Type.END_MARKER ->
                        drawEndMarker(
                            graphics,
                            position,
                            nodeLayout.width,
                            nodeLayout.height,
                            selected
                        )

                    DiagramNode.Type.CHOICE ->
                        drawChoiceNode(
                            graphics,
                            node,
                            position,
                            nodeLayout.width,
                            nodeLayout.height,
                            selected,
                            incomingSource
                        )

                    DiagramNode.Type.STATE ->
                        drawStateNode(
                            graphics,
                            node,
                            position,
                            nodeLayout.width,
                            nodeLayout.height,
                            selected,
                            incomingSource
                        )
                }
            } finally {
                graphics.dispose()
            }
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------


    /**
     * Рисует обычное состояние с учётом его роли
     * в текущей подсветке.
     */
    private fun drawStateNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean,
        incomingSource: Boolean
    ) {
        val hovered = node.id == interaction.hoveredNodeId

        g2.color = when {
            selected -> DiagramColors.SELECTED_STATE_BACKGROUND
            incomingSource -> DiagramColors.INCOMING_STATE_BACKGROUND
            else -> DiagramColors.STATE_BACKGROUND
        }

        g2.fillRoundRect(
            position.x,
            position.y,
            width,
            height,
            14,
            14
        )

        g2.color = when {
            selected -> DiagramColors.SELECTED_BORDER
            incomingSource -> DiagramColors.INCOMING_STATE_BORDER
            hovered -> DiagramColors.STATE_HOVER_BORDER
            else -> DiagramColors.STATE_BORDER
        }

        g2.stroke = BasicStroke(
            when {
                selected -> NodeRenderDimensions.SELECTED_NODE_WIDTH
                incomingSource -> DiagramColors.INCOMING_NODE_BORDER_WIDTH
                hovered -> NodeRenderDimensions.HOVER_NODE_WIDTH
                else -> NodeRenderDimensions.NORMAL_NODE_WIDTH
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

        val oldFont = g2.font
        g2.font = oldFont.deriveFont(Font.PLAIN, 13f)

        val metrics = g2.fontMetrics
        val textX = position.x +
                (width - metrics.stringWidth(node.label)) / 2
        val textY = position.y +
                (height - metrics.height) / 2 + metrics.ascent

        g2.color = when {
            selected -> DiagramColors.SELECTED_STATE_TEXT
            incomingSource -> DiagramColors.INCOMING_STATE_TEXT
            else -> DiagramColors.STATE_TEXT
        }

        g2.drawString(node.label, textX, textY)
        g2.font = oldFont
    }


    // -------------------------------------------------------------------------
    // Choice
    // -------------------------------------------------------------------------

    /**
     * Отрисовывает choice-состояние.
     */
    private fun drawChoiceNode(
        g2: Graphics2D,
        node: DiagramNode,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean,
        incomingSource: Boolean
    ) {
        val centerX = position.x + width / 2
        val centerY = position.y + height / 2
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

        val hovered = node.id == interaction.hoveredNodeId
        g2.color = if (selected) {
            DiagramColors.SELECTED_STATE_BACKGROUND
        } else {
            DiagramColors.CHOICE_BACKGROUND
        }

        g2.fillPolygon(xPoints, yPoints, 4)
        g2.color = when {
            selected -> DiagramColors.SELECTED_BORDER
            incomingSource -> DiagramColors.INCOMING_STATE_BORDER
            hovered -> DiagramColors.CHOICE_HOVER_BORDER
            else -> DiagramColors.CHOICE_BORDER
        }

        g2.stroke = BasicStroke(
            when {
                selected -> NodeRenderDimensions.SELECTED_NODE_WIDTH
                incomingSource -> DiagramColors.INCOMING_NODE_BORDER_WIDTH
                hovered -> NodeRenderDimensions.HOVER_NODE_WIDTH
                else -> NodeRenderDimensions.NORMAL_NODE_WIDTH
            }
        )

        g2.drawPolygon(xPoints, yPoints, 4)
        g2.color = DiagramColors.CHOICE_TEXT
        g2.drawString(node.label, centerX + halfSize + 12, centerY + 5
        )
    }

    // -------------------------------------------------------------------------
    // Initial
    // -------------------------------------------------------------------------

    /**
     * Отрисовывает initial-маркер.
     */
    private fun drawInitialMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {
        val centerX = position.x + width / 2
        val centerY = position.y + height / 2

        val radius =
            if (selected) {
                10
            } else {
                8
            }

        g2.color =
            if (selected) {
                DiagramColors.SELECTED_BORDER
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

    /**
     * Отрисовывает end-маркер.
     */
    private fun drawEndMarker(
        g2: Graphics2D,
        position: Point,
        width: Int,
        height: Int,
        selected: Boolean
    ) {
        val centerX = position.x + width / 2
        val centerY = position.y + height / 2

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
                DiagramColors.SELECTED_BORDER
            } else {
                JBColor.foreground()
            }

        g2.stroke =
            BasicStroke(
                if (selected) {
                    NodeRenderDimensions.SELECTED_NODE_WIDTH
                } else {
                    NodeRenderDimensions.NORMAL_NODE_WIDTH
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

}