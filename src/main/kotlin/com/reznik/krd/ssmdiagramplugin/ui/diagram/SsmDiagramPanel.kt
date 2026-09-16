package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.builder.DiagramGraphBuilder
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.EdgeLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.HierarchicalLayoutEngine
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import com.reznik.krd.ssmdiagramplugin.ui.diagram.transform.ReadableGraphTransformer
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class SsmDiagramPanel(
    private val project: Project
) : JPanel() {

    private val graphBuilder =
        DiagramGraphBuilder()

    private val graphTransformer =
        ReadableGraphTransformer()

    private val layoutEngine =
        HierarchicalLayoutEngine()

    /*
     * Hover.
     */
    private var hoveredNodeId: String? = null
    private var hoveredGuardEdgeId: String? = null

    /*
     * Реальные области отрисованных guard labels.
     *
     * key   = edge.id
     * value = Rectangle label
     */
    private val guardHitAreas =
        mutableMapOf<String, Rectangle>()

    private var graph: DiagramGraph? = null
    private var layout: DiagramLayout? = null

    /*
     * Zoom.
     */
    private var zoom = 1.0

    private val minZoom = 0.25
    private val maxZoom = 2.5
    private val zoomStep = 0.10

    init {

        background =
            JBColor.PanelBackground

        /*
         * Zoom:
         *
         * macOS         -> Cmd + wheel
         * Windows/Linux -> Ctrl + wheel
         */
        addMouseWheelListener { event ->

            val zoomRequested =
                event.isControlDown ||
                        event.isMetaDown

            if (!zoomRequested) {
                return@addMouseWheelListener
            }

            event.consume()

            if (event.wheelRotation < 0) {
                zoomIn()
            } else {
                zoomOut()
            }
        }

        /*
         * Click.
         */
        addMouseListener(
            object : MouseAdapter() {

                override fun mouseClicked(
                    event: MouseEvent
                ) {
                    handleMouseClick(event)
                }

                override fun mouseExited(
                    event: MouseEvent
                ) {
                    clearHover()
                }
            }
        )

        /*
         * Hover.
         */
        addMouseMotionListener(
            object : MouseMotionAdapter() {

                override fun mouseMoved(
                    event: MouseEvent
                ) {
                    handleMouseMove(event)
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    private fun handleMouseMove(
        event: MouseEvent
    ) {

        /*
         * Сначала проверяем guard.
         *
         * Он имеет приоритет перед node.
         */
        val guardEdge =
            findGuardEdgeAt(
                mouseX = event.x,
                mouseY = event.y
            )

        /*
         * Hover показываем только если
         * guard реально ведёт на PSI class.
         *
         * Например "otherwise" не кликабелен.
         */
        val clickableGuard =
            guardEdge?.takeIf {
                it.guard?.targetClass != null
            }

        if (clickableGuard != null) {

            val changed =
                hoveredGuardEdgeId != clickableGuard.id ||
                        hoveredNodeId != null

            hoveredGuardEdgeId =
                clickableGuard.id

            hoveredNodeId =
                null

            cursor =
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )

            if (changed) {
                repaint()
            }

            return
        }

        /*
         * Guard не найден.
         *
         * Проверяем state.
         */
        val node =
            findNodeAt(
                mouseX = event.x,
                mouseY = event.y
            )

        val clickableNode =
            node?.takeIf {
                it.state?.source != null
            }

        val newHoveredNodeId =
            clickableNode?.id

        val changed =
            hoveredNodeId != newHoveredNodeId ||
                    hoveredGuardEdgeId != null

        hoveredGuardEdgeId =
            null

        hoveredNodeId =
            newHoveredNodeId

        cursor =
            if (clickableNode != null) {
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )
            } else {
                Cursor.getDefaultCursor()
            }

        if (changed) {
            repaint()
        }
    }

    private fun clearHover() {

        val changed =
            hoveredNodeId != null ||
                    hoveredGuardEdgeId != null

        hoveredNodeId = null
        hoveredGuardEdgeId = null

        cursor =
            Cursor.getDefaultCursor()

        if (changed) {
            repaint()
        }
    }

    private fun handleMouseClick(
        event: MouseEvent
    ) {

        /*
         * Только левая кнопка.
         */
        if (event.button != MouseEvent.BUTTON1) {
            return
        }

        /*
         * Сначала guard.
         */
        val guardEdge =
            findGuardEdgeAt(
                mouseX = event.x,
                mouseY = event.y
            )

        val guardClass =
            guardEdge
                ?.guard
                ?.targetClass

        if (guardClass != null) {

            navigateToPsiElement(
                guardClass
            )

            return
        }

        /*
         * Потом state.
         */
        val node =
            findNodeAt(
                mouseX = event.x,
                mouseY = event.y
            ) ?: return

        navigateToNode(node)
    }

    // -------------------------------------------------------------------------
    // Hit testing
    // -------------------------------------------------------------------------

    private fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? {

        val graph =
            this.graph ?: return null

        val layout =
            this.layout ?: return null

        /*
         * Координаты MouseEvent находятся
         * в screen-space.
         *
         * Layout находится в diagram-space.
         *
         * Поэтому учитываем zoom.
         */
        val diagramX =
            (mouseX / zoom).toInt()

        val diagramY =
            (mouseY / zoom).toInt()

        return graph.nodes.firstOrNull { node ->

            val nodeLayout =
                layout.nodes[node.id]
                    ?: return@firstOrNull false

            diagramX >= nodeLayout.x &&
                    diagramX <= nodeLayout.x + nodeLayout.width &&
                    diagramY >= nodeLayout.y &&
                    diagramY <= nodeLayout.y + nodeLayout.height
        }
    }

    private fun findGuardEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val graph =
            this.graph ?: return null

        /*
         * Возвращаем координаты мыши
         * обратно в diagram-space.
         */
        val diagramX =
            (mouseX / zoom).toInt()

        val diagramY =
            (mouseY / zoom).toInt()

        val edgeId =
            guardHitAreas
                .entries
                .firstOrNull { (_, bounds) ->

                    bounds.contains(
                        diagramX,
                        diagramY
                    )
                }
                ?.key
                ?: return null

        return graph.edges
            .firstOrNull {
                it.id == edgeId
            }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private fun navigateToNode(
        node: DiagramNode
    ) {

        val source =
            node.state?.source
                ?: return

        navigateToPsiElement(
            source
        )
    }

    private fun navigateToPsiElement(
        element: PsiElement
    ) {

        val virtualFile =
            element.containingFile
                ?.virtualFile
                ?: return

        OpenFileDescriptor(
            project,
            virtualFile,
            element.textOffset
        ).navigate(true)
    }

    // -------------------------------------------------------------------------
    // Zoom
    // -------------------------------------------------------------------------

    private fun zoomIn() {

        setZoom(
            zoom + zoomStep
        )
    }

    private fun zoomOut() {

        setZoom(
            zoom - zoomStep
        )
    }

    private fun setZoom(
        value: Double
    ) {

        zoom =
            value.coerceIn(
                minZoom,
                maxZoom
            )

        updatePreferredSize()

        revalidate()
        repaint()
    }

    private fun updatePreferredSize() {

        val layout =
            this.layout ?: return

        preferredSize =
            Dimension(
                (layout.size.width * zoom).toInt(),
                (layout.size.height * zoom).toInt()
            )
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    fun setModel(
        model: StateMachineModel
    ) {

        /*
         * Канонический граф:
         *
         * один logical state = один node.
         */
        val logicalGraph =
            graphBuilder.build(model)

        /*
         * Граф для отображения.
         *
         * Здесь могут появляться visual copies
         * одного logical state.
         */
        val visualGraph =
            graphTransformer.transform(
                logicalGraph
            )

        val layout =
            layoutEngine.layout(
                visualGraph
            )

        this.graph =
            visualGraph

        this.layout =
            layout

        /*
         * Сбрасываем hover от предыдущей схемы.
         */
        hoveredNodeId = null
        hoveredGuardEdgeId = null

        guardHitAreas.clear()

        updatePreferredSize()

        revalidate()
        repaint()
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    override fun paintComponent(
        graphics: Graphics
    ) {

        super.paintComponent(graphics)

        val graph =
            this.graph ?: return

        val layout =
            this.layout ?: return

        val g2 =
            graphics.create() as Graphics2D

        try {

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )

            g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )

            /*
             * Весь renderer работает
             * в diagram-space.
             */
            g2.scale(
                zoom,
                zoom
            )

            /*
             * Каждый paint заново создаёт
             * реальные hit areas labels.
             */
            guardHitAreas.clear()

            /*
             * Сначала edges.
             *
             * Nodes рисуем поверх edges.
             */
            drawEdges(
                g2,
                graph,
                layout
            )

            drawNodes(
                g2,
                graph,
                layout
            )

        } finally {
            g2.dispose()
        }
    }

    // -------------------------------------------------------------------------
    // Edges
    // -------------------------------------------------------------------------

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
         *
         * Пока event только отображаем.
         * Интерактивность добавим следующим этапом.
         */
        edge.event?.let { event ->

            edgeLayout.eventLabelPosition
                ?.let { position ->

                    drawLabelChip(
                        g2 = g2,
                        text = event,
                        position = position,
                        kind = LabelKind.EVENT
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
    }

    /**
     * Label chip.
     *
     * Одновременно:
     *
     * 1. рисует фон;
     * 2. рисует текст;
     * 3. для guard сохраняет Rectangle;
     * 4. подсвечивает guard при hover.
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

        /*
         * Event немного заметнее.
         *
         * Guard компактнее.
         */
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
         *
         * Оставляем ту же систему координат,
         * которая была у тебя до интерактивности,
         * чтобы labels визуально не прыгнули.
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
         * Запоминаем hit area guard.
         *
         * В том числе для otherwise.
         * Но otherwise потом не считается clickable,
         * потому что edge.guard == null.
         */
        if (
            kind == LabelKind.GUARD &&
            edgeId != null
        ) {
            guardHitAreas[edgeId] =
                bounds
        }

        val hovered =
            kind == LabelKind.GUARD &&
                    edgeId != null &&
                    edgeId == hoveredGuardEdgeId

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
         * При hover добавляем небольшую рамку.
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
            }

        g2.drawString(
            text,
            chipX + paddingX,
            position.y
        )

        /*
         * Возвращаем Graphics2D в исходное состояние.
         */
        g2.font =
            oldFont

        g2.stroke =
            oldStroke
    }

    // -------------------------------------------------------------------------
    // Nodes
    // -------------------------------------------------------------------------

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
            node.id == hoveredNodeId

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
            node.id == hoveredNodeId

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
        GUARD
    }
}