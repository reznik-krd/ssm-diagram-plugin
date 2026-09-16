package com.reznik.krd.ssmdiagramplugin.ui.diagram.render

import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionHitArea
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionKey
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.EdgeLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Renders transition lines, labels, arrows and action chips. */
internal class EdgeRenderer(
    private val interaction: DiagramInteractionState,
    private val hitRegistry: DiagramHitRegistry
) {

    fun render(g2: Graphics2D, graph: DiagramGraph, layout: DiagramLayout) {
        drawEdges(g2, graph, layout)
    }

    private fun drawEdges(
        g2: Graphics2D,
        graph: DiagramGraph,
        layout: DiagramLayout
    ) {

        g2.stroke =
            BasicStroke(1.4f)

        graph.edges.forEach { edge ->

            val edgeLayout =
                layout.edges[edge.id]
                    ?: return@forEach

            if (edgeLayout.points.size < 2) {
                return@forEach
            }

            /*
             * Polyline.
             */
            g2.color =
                JBColor.GRAY

            edgeLayout.points
                .zipWithNext()
                .forEach { (start, end) ->

                    g2.drawLine(
                        start.x,
                        start.y,
                        end.x,
                        end.y
                    )
                }

            /*
             * Arrowhead.
             */
            val beforeEnd =
                edgeLayout.points[
                    edgeLayout.points.size - 2
                ]

            val end =
                edgeLayout.points.last()

            drawArrowHead(
                g2 = g2,
                from = beforeEnd,
                to = end
            )

            /*
             * Labels.
             */
            drawEdgeLabels(
                g2 = g2,
                edge = edge,
                edgeLayout = edgeLayout
            )
        }
    }

    private fun drawEdgeLabels(
        g2: Graphics2D,
        edge: DiagramEdge,
        edgeLayout: EdgeLayout
    ) {

        /*
         * Технические INITIAL/END
         * не подписываем.
         */
        if (
            edge.type == DiagramEdge.Type.INITIAL ||
            edge.type == DiagramEdge.Type.END
        ) {
            return
        }

        /*
         * Event.
         */
        edge.event?.let { event ->

            edgeLayout.eventLabelPosition
                ?.let { position ->

                    drawLabelChip(
                        g2 = g2,
                        text = event,
                        position = position,
                        kind = LabelKind.EVENT,
                        edgeId = edge.id
                    )
                }
        }

        /*
         * Guard.
         */
        val guardText =
            when {

                edge.guard != null &&
                        edge.type ==
                        DiagramEdge.Type.CHOICE_BRANCH -> {

                    edge.guard.name
                }

                edge.guard != null -> {

                    "[${edge.guard.name}]"
                }

                edge.type ==
                        DiagramEdge.Type.CHOICE_BRANCH -> {

                    "otherwise"
                }

                else ->
                    null
            }

        guardText?.let { text ->

            edgeLayout.guardLabelPosition
                ?.let { position ->

                    drawLabelChip(
                        g2 = g2,
                        text = text,
                        position = position,
                        kind = LabelKind.GUARD,
                        edgeId = edge.id
                    )
                }
        }

        /*
         * Actions.
         *
         * Error actions намеренно
         * не отображаем.
         *
         * Каждый action имеет собственный index,
         * поэтому несколько actions одного edge
         * остаются независимыми.
         */
        edge.actions
            .zip(edgeLayout.actionLabelPositions)
            .forEachIndexed { index, (ssmAction, position) ->

                drawActionLabel(
                    g2 = g2,
                    edge = edge,
                    actionIndex = index,
                    action = ssmAction,
                    position = position
                )
            }
    }

    private fun drawActionLabel(
        g2: Graphics2D,
        edge: DiagramEdge,
        actionIndex: Int,
        action: SsmAction,
        position: Point
    ) {

        val oldFont =
            g2.font

        val oldColor =
            g2.color

        val oldStroke =
            g2.stroke

        g2.font =
            oldFont.deriveFont(
                Font.PLAIN,
                11f
            )

        val text =
            "⚡ ${action.action.name}"

        val metrics =
            g2.fontMetrics

        val textWidth =
            metrics.stringWidth(text)

        val textHeight =
            metrics.height

        val horizontalPadding = 7
        val verticalPadding = 3

        val bounds =
            Rectangle(
                position.x -
                        textWidth / 2 -
                        horizontalPadding,

                position.y -
                        metrics.ascent -
                        verticalPadding,

                textWidth +
                        horizontalPadding * 2,

                textHeight +
                        verticalPadding * 2
            )

        /*
         * Запоминаем реальную область именно
         * этого конкретного action.
         */
        hitRegistry.actionAreas +=
            ActionHitArea(
                edgeId = edge.id,
                actionIndex = actionIndex,
                bounds = bounds
            )

        val hovered =
            interaction.hoveredAction ==
                    ActionKey(edge.id, actionIndex)

        /*
         * Background.
         */
        g2.color =
            if (hovered) {
                JBColor(
                    Color(225, 235, 250),
                    Color(65, 75, 90)
                )
            } else {
                JBColor.PanelBackground
            }

        g2.fillRoundRect(
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height,
            8,
            8
        )

        /*
         * Hover border.
         */
        if (hovered) {

            g2.color =
                JBColor.GRAY

            g2.stroke =
                BasicStroke(1f)

            g2.drawRoundRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                8,
                8
            )
        }

        /*
         * Text.
         */
        g2.color =
            if (hovered) {
                JBColor.foreground()
            } else {
                JBColor.GRAY
            }

        g2.drawString(
            text,
            position.x - textWidth / 2,
            position.y
        )

        g2.font = oldFont
        g2.color = oldColor
        g2.stroke = oldStroke
    }

    /**
     * Общий label chip для EVENT / GUARD.
     */
    private fun drawLabelChip(
        g2: Graphics2D,
        text: String,
        position: Point,
        kind: LabelKind,
        edgeId: String? = null
    ) {

        val oldFont =
            g2.font

        val oldStroke =
            g2.stroke

        val oldColor =
            g2.color

        g2.font =
            when (kind) {

                LabelKind.EVENT ->
                    oldFont.deriveFont(
                        Font.PLAIN,
                        12f
                    )

                LabelKind.GUARD ->
                    oldFont.deriveFont(
                        Font.PLAIN,
                        11f
                    )

                LabelKind.ACTION ->
                    oldFont.deriveFont(
                        Font.PLAIN,
                        11f
                    )
            }

        val metrics =
            g2.fontMetrics

        val paddingX = 6
        val paddingY = 3

        val textWidth =
            metrics.stringWidth(text)

        val chipWidth =
            textWidth +
                    paddingX * 2

        val chipHeight =
            metrics.height +
                    paddingY * 2

        /*
         * position.x — центр label.
         */
        val chipX =
            position.x -
                    chipWidth / 2

        /*
         * position.y — baseline текста.
         */
        val chipY =
            position.y -
                    metrics.ascent -
                    paddingY

        val bounds =
            Rectangle(
                chipX,
                chipY,
                chipWidth,
                chipHeight
            )

        /*
         * Hit areas.
         */
        if (edgeId != null) {

            when (kind) {

                LabelKind.EVENT ->
                    hitRegistry.eventAreas[edgeId] =
                        bounds

                LabelKind.GUARD ->
                    hitRegistry.guardAreas[edgeId] =
                        bounds

                /*
                 * ACTION имеет отдельный renderer,
                 * потому что на edge их может быть несколько.
                 */
                LabelKind.ACTION ->
                    Unit
            }
        }

        val hovered =
            when (kind) {

                LabelKind.EVENT ->
                    edgeId != null &&
                            edgeId ==
                            interaction.hoveredEventEdgeId

                LabelKind.GUARD ->
                    edgeId != null &&
                            edgeId ==
                            interaction.hoveredGuardEdgeId

                LabelKind.ACTION ->
                    false
            }

        /*
         * Background.
         */
        g2.color =
            if (hovered) {
                JBColor(
                    Color(225, 235, 250),
                    Color(65, 75, 90)
                )
            } else {
                JBColor.PanelBackground
            }

        g2.fillRoundRect(
            chipX,
            chipY,
            chipWidth,
            chipHeight,
            8,
            8
        )

        /*
         * Hover border.
         */
        if (hovered) {

            g2.color =
                JBColor.GRAY

            g2.stroke =
                BasicStroke(1f)

            g2.drawRoundRect(
                chipX,
                chipY,
                chipWidth,
                chipHeight,
                8,
                8
            )
        }

        /*
         * Text.
         */
        g2.color =
            when (kind) {

                LabelKind.EVENT ->
                    JBColor.foreground()

                LabelKind.GUARD ->
                    if (hovered) {
                        JBColor.foreground()
                    } else {
                        JBColor.GRAY
                    }

                LabelKind.ACTION ->
                    JBColor.foreground()
            }

        g2.drawString(
            text,
            chipX + paddingX,
            position.y
        )

        g2.font = oldFont
        g2.stroke = oldStroke
        g2.color = oldColor
    }

    // -------------------------------------------------------------------------
    // Nodes
    // -------------------------------------------------------------------------

    private fun drawArrowHead(
        g2: Graphics2D,
        from: Point,
        to: Point
    ) {

        val angle =
            atan2(
                (to.y - from.y).toDouble(),
                (to.x - from.x).toDouble()
            )

        val arrowLength = 10.0

        val angleOffset =
            Math.toRadians(28.0)

        val firstX =
            (
                    to.x -
                            arrowLength *
                            cos(angle - angleOffset)
                    ).toInt()

        val firstY =
            (
                    to.y -
                            arrowLength *
                            sin(angle - angleOffset)
                    ).toInt()

        val secondX =
            (
                    to.x -
                            arrowLength *
                            cos(angle + angleOffset)
                    ).toInt()

        val secondY =
            (
                    to.y -
                            arrowLength *
                            sin(angle + angleOffset)
                    ).toInt()

        g2.drawLine(
            to.x,
            to.y,
            firstX,
            firstY
        )

        g2.drawLine(
            to.x,
            to.y,
            secondX,
            secondY
        )
    }



    private enum class LabelKind {
        EVENT,
        GUARD,
        ACTION
    }
}
