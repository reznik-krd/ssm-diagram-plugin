package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiElement
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import javax.swing.JPanel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.intellij.openapi.project.Project
import java.awt.event.MouseMotionAdapter

class SsmDiagramPanel(
    private val project: Project
) : JPanel() {

    private val graphBuilder =
        DiagramGraphBuilder()

    private val graphTransformer =
        ReadableGraphTransformer()

    private val layoutEngine =
        HierarchicalLayoutEngine()
    private var hoveredNodeId: String? = null

    private var graph: DiagramGraph? = null

    private var layout: DiagramLayout? = null

    private var zoom = 1.0
    private val minZoom = 0.25
    private val maxZoom = 2.5
    private val zoomStep = 0.10

    init {
        background = JBColor.PanelBackground

        addMouseWheelListener { event ->

            /*
             * macOS -> Meta (Cmd)
             * Windows/Linux -> Ctrl
             */
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

    private fun handleMouseMove(
        event: MouseEvent
    ) {

        val node =
            findNodeAt(
                mouseX = event.x,
                mouseY = event.y
            )

        /*
         * INITIAL / END технически тоже являются nodes,
         * но кликать по ним сейчас некуда.
         *
         * Поэтому hover показываем только для state,
         * у которого действительно есть source.
         */
        val clickableNode =
            node?.takeIf {
                it.state?.source != null
            }

        val newHoveredNodeId =
            clickableNode?.id

        /*
         * repaint нужен только если hover
         * реально изменился.
         */
        if (hoveredNodeId != newHoveredNodeId) {

            hoveredNodeId =
                newHoveredNodeId

            repaint()
        }

        cursor =
            if (clickableNode != null) {
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )
            } else {
                Cursor.getDefaultCursor()
            }
    }

    private fun clearHover() {

        if (hoveredNodeId == null) {
            return
        }

        hoveredNodeId = null

        cursor =
            Cursor.getDefaultCursor()

        repaint()
    }

    private fun handleMouseClick(
        event: MouseEvent
    ) {

        /*
         * Нас интересует только левая кнопка.
         */
        if (event.button != MouseEvent.BUTTON1) {
            return
        }

        val node =
            findNodeAt(
                mouseX = event.x,
                mouseY = event.y
            ) ?: return

        navigateToNode(node)
    }

    private fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? {

        val graph =
            this.graph ?: return null

        val layout =
            this.layout ?: return null

        /*
         * Переводим экранные координаты
         * обратно в координаты DiagramLayout.
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

    private fun navigateToNode(
        node: DiagramNode
    ) {

        val source =
            node.state?.source
                ?: return

        navigateToPsiElement(source)
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
            java.awt.Dimension(
                (layout.size.width * zoom).toInt(),
                (layout.size.height * zoom).toInt()
            )
    }

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

        updatePreferredSize()

        revalidate()
        repaint()
    }

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

            g2.scale(
                zoom,
                zoom
            )

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
             * Рисуем polyline.
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
             * Arrowhead рисуем только
             * на последнем сегменте.
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
             * Labels поверх линии.
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
         * Технические INITIAL/END edges
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
                        kind = LabelKind.EVENT
                    )
                }
        }

        /*
         * Guard.
         *
         * Для choice:
         *
         * first(..., guard)
         * then(..., guard)
         *
         * показываем просто имя guard.
         *
         * Для last(...)
         * показываем otherwise.
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
                        kind = LabelKind.GUARD
                    )
                }
        }
    }

    /**
     * Рисуем подпись с собственным фоном.
     *
     * Это важно, потому что иначе:
     *
     * -------------------------
     *      SOME_LONG_EVENT
     * -------------------------
     *
     * линия проходит прямо через текст.
     */
    private fun drawLabelChip(
        g2: Graphics2D,
        text: String,
        position: Point,
        kind: LabelKind
    ) {

        val oldFont =
            g2.font

        /*
         * Event немного заметнее.
         *
         * Guard делаем чуть компактнее.
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
         * position.x считаем центром label.
         */
        val chipX =
            position.x -
                    chipWidth / 2

        /*
         * position.y — baseline-ish anchor.
         */
        val chipY =
            position.y -
                    metrics.ascent -
                    paddingY

        /*
         * Сначала перекрываем проходящую
         * под label линию.
         */
        g2.color =
            JBColor.PanelBackground

        g2.fillRoundRect(
            chipX,
            chipY,
            chipWidth,
            chipHeight,
            8,
            8
        )

        /*
         * Затем текст.
         */
        g2.color =
            when (kind) {

                LabelKind.EVENT ->
                    JBColor.foreground()

                LabelKind.GUARD ->
                    JBColor.GRAY
            }

        g2.drawString(
            text,
            chipX + paddingX,
            position.y
        )

        g2.font =
            oldFont
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
            node.id == hoveredNodeId

        g2.color =
            if (hovered) {
                JBColor(
                    java.awt.Color(235, 242, 252),
                    java.awt.Color(55, 65, 78)
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
                    (width -
                            metrics.stringWidth(
                                node.label
                            )) / 2

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
                    java.awt.Color(235, 242, 252),
                    java.awt.Color(55, 65, 78)
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
            JBColor.GRAY

        g2.stroke =
            BasicStroke(1.4f)

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