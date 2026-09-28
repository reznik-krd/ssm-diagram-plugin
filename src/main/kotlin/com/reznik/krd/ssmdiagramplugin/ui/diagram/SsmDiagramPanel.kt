package com.reznik.krd.ssmdiagramplugin.ui.diagram

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.builder.DiagramGraphBuilder
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionHitArea
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionKey
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramNavigator
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.HierarchicalLayoutEngine
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import com.reznik.krd.ssmdiagramplugin.ui.diagram.render.DiagramRenderer
import com.reznik.krd.ssmdiagramplugin.ui.diagram.transform.ReadableGraphTransformer
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import kotlin.math.abs
import javax.swing.JPanel

/**
 * Swing surface that displays an analyzed Spring StateMachine diagram.
 *
 * The panel coordinates graph preparation, zoom, mouse interaction and rendering while
 * delegating drawing, hit-area storage and source navigation to focused collaborators.
 */
class SsmDiagramPanel(
    project: Project
) : JPanel() {

    private val graphBuilder =
        DiagramGraphBuilder()

    private val graphTransformer =
        ReadableGraphTransformer()

    private val layoutEngine =
        HierarchicalLayoutEngine()

    private val interaction =
        DiagramInteractionState()

    private val hitRegistry =
        DiagramHitRegistry()

    private val navigator =
        DiagramNavigator(project)

    private val renderer =
        DiagramRenderer(
            interaction,
            hitRegistry
        )

    private var graph: DiagramGraph? = null

    private var layout: DiagramLayout? = null

    private var zoom =
        DEFAULT_ZOOM

    private var zoomChangedListener:
            ((Double) -> Unit)? = null

    init {
        background =
            JBColor.PanelBackground

        installMouseHandlers()
    }

    // -------------------------------------------------------------------------
    // Zoom
    // -------------------------------------------------------------------------

    /**
     * Registers a listener invoked whenever the diagram zoom changes.
     */
    fun setZoomChangedListener(
        listener: (Double) -> Unit
    ) {
        zoomChangedListener = listener
        listener(zoom)
    }

    /**
     * Increases the diagram scale by one zoom step.
     */
    fun zoomIn() =
        setZoom(
            zoom + ZOOM_STEP
        )

    /**
     * Decreases the diagram scale by one zoom step.
     */
    fun zoomOut() =
        setZoom(
            zoom - ZOOM_STEP
        )

    /**
     * Restores the diagram to 100% scale.
     */
    fun resetZoom() =
        setZoom(
            DEFAULT_ZOOM
        )

    /**
     * Fits the complete diagram into the supplied viewport
     * while preserving aspect ratio.
     */
    fun fitToViewport(
        viewportSize: Dimension
    ) {

        val currentLayout =
            layout ?: return

        if (
            currentLayout.size.width <= 0 ||
            currentLayout.size.height <= 0 ||
            viewportSize.width <= 0 ||
            viewportSize.height <= 0
        ) {
            return
        }

        val availableWidth =
            (viewportSize.width - FIT_PADDING)
                .coerceAtLeast(1)

        val availableHeight =
            (viewportSize.height - FIT_PADDING)
                .coerceAtLeast(1)

        val widthZoom =
            availableWidth.toDouble() /
                    currentLayout.size.width

        val heightZoom =
            availableHeight.toDouble() /
                    currentLayout.size.height

        setZoom(
            minOf(
                widthZoom,
                heightZoom
            )
        )
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    /**
     * Builds and displays a visual graph for the supplied analyzed state machine.
     */
    fun setModel(
        model: StateMachineModel
    ) {

        val logicalGraph =
            graphBuilder.build(model)

        val visualGraph =
            graphTransformer.transform(
                logicalGraph
            )

        graph =
            visualGraph

        layout =
            layoutEngine.layout(
                visualGraph
            )

        interaction.clear()
        hitRegistry.clear()

        updatePreferredSize()

        revalidate()
        repaint()
    }

    /**
     * Removes the current diagram and restores the default view state.
     */
    fun clear() {

        graph = null
        layout = null

        interaction.clear()
        hitRegistry.clear()

        zoom =
            DEFAULT_ZOOM

        zoomChangedListener
            ?.invoke(zoom)

        preferredSize =
            Dimension(
                0,
                0
            )

        cursor =
            Cursor.getDefaultCursor()

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

        val currentGraph =
            graph ?: return

        val currentLayout =
            layout ?: return

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

            renderer.render(
                g2,
                currentGraph,
                currentLayout
            )

        } finally {
            g2.dispose()
        }
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    private fun installMouseHandlers() {

        addMouseWheelListener { event ->

            if (
                !event.isControlDown &&
                !event.isMetaDown
            ) {
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

        /*
         * Action.
         */
        val actionArea =
            findActionAt(
                event.x,
                event.y
            )

        val action =
            actionArea?.let(
                ::findAction
            )

        if (
            actionArea != null &&
            action?.action?.targetClass != null
        ) {

            updateHover(
                action =
                    ActionKey(
                        actionArea.edgeId,
                        actionArea.actionIndex
                    )
            )

            return
        }

        /*
         * Event.
         */
        val eventEdge =
            findEventEdgeAt(
                event.x,
                event.y
            )

        if (
            eventEdge != null &&
            eventEdge.eventElement != null
        ) {

            updateHover(
                eventEdgeId =
                    eventEdge.id
            )

            return
        }

        /*
         * Guard.
         */
        val guardEdge =
            findGuardEdgeAt(
                event.x,
                event.y
            )

        if (
            guardEdge != null &&
            guardEdge.guard
                ?.targetClass != null
        ) {

            updateHover(
                guardEdgeId =
                    guardEdge.id
            )

            return
        }

        /*
         * State node.
         */
        val node =
            findNodeAt(
                event.x,
                event.y
            )
                ?.takeIf {
                    it.state?.source != null
                }

        if (node != null) {

            updateHover(
                nodeId = node.id
            )

            return
        }

        /*
         * Edge.
         *
         * We do not select it on hover,
         * but show that the line is clickable.
         */
        val edge =
            findDiagramEdgeAt(
                event.x,
                event.y
            )

        if (edge != null) {

            updateHover(
                clickable = true
            )

            return
        }

        updateHover(
            clickable = false
        )
    }

    private fun updateHover(
        nodeId: String? = null,
        eventEdgeId: String? = null,
        guardEdgeId: String? = null,
        action: ActionKey? = null,
        clickable: Boolean = true
    ) {

        val changed =
            interaction.hoveredNodeId != nodeId ||
                    interaction.hoveredEventEdgeId != eventEdgeId ||
                    interaction.hoveredGuardEdgeId != guardEdgeId ||
                    interaction.hoveredAction != action

        interaction.hoveredNodeId =
            nodeId

        interaction.hoveredEventEdgeId =
            eventEdgeId

        interaction.hoveredGuardEdgeId =
            guardEdgeId

        interaction.hoveredAction =
            action

        cursor =
            if (clickable) {
                HAND_CURSOR
            } else {
                Cursor.getDefaultCursor()
            }

        if (changed) {
            repaint()
        }
    }

    private fun clearHover() {

        val changed =
            interaction.hasHover()

        /*
         * ВАЖНО:
         *
         * selectedEdgeId здесь НЕ очищаем.
         * Выбранное ребро должно оставаться
         * выбранным после ухода мыши.
         */
        interaction.clearHover()

        cursor =
            Cursor.getDefaultCursor()

        if (changed) {
            repaint()
        }
    }

    // -------------------------------------------------------------------------
    // Click
    // -------------------------------------------------------------------------

    private fun handleMouseClick(
        event: MouseEvent
    ) {

        if (
            event.button !=
            MouseEvent.BUTTON1
        ) {
            return
        }

        /*
         * Action navigation has the highest priority.
         */
        val actionClass =
            findActionAt(
                event.x,
                event.y
            )
                ?.let(::findAction)
                ?.action
                ?.targetClass

        if (actionClass != null) {
            navigator.navigateTo(
                actionClass
            )
            return
        }

        /*
         * Event navigation.
         */
        val eventElement =
            findEventEdgeAt(
                event.x,
                event.y
            )
                ?.eventElement

        if (eventElement != null) {
            navigator.navigateTo(
                eventElement
            )
            return
        }

        /*
         * Guard navigation.
         */
        val guardClass =
            findGuardEdgeAt(
                event.x,
                event.y
            )
                ?.guard
                ?.targetClass

        if (guardClass != null) {
            navigator.navigateTo(
                guardClass
            )
            return
        }

        /*
         * Node navigation.
         *
         * Node has priority over edge because
         * some problematic routes may currently
         * visually pass through a state.
         */
        val node =
            findNodeAt(
                event.x,
                event.y
            )

        if (node != null) {

            node.state
                ?.source
                ?.let(
                    navigator::navigateTo
                )

            return
        }

        /*
         * Edge selection.
         */
        val edge =
            findDiagramEdgeAt(
                event.x,
                event.y
            )

        if (edge != null) {

            interaction.selectedEdgeId =
                if (
                    interaction.selectedEdgeId ==
                    edge.id
                ) {
                    /*
                     * Clicking the selected edge again
                     * deselects it.
                     */
                    null
                } else {
                    edge.id
                }

            repaint()
            return
        }

        /*
         * Empty space clears edge selection.
         */
        if (
            interaction.selectedEdgeId != null
        ) {

            interaction.selectedEdgeId =
                null

            repaint()
        }
    }

    // -------------------------------------------------------------------------
    // Edge hit testing
    // -------------------------------------------------------------------------

    /**
     * Finds an edge close enough to the mouse position.
     *
     * The actual line may only be 1-2 px wide,
     * therefore EDGE_CLICK_TOLERANCE creates
     * a larger invisible click area.
     */
    private fun findDiagramEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val currentGraph =
            graph ?: return null

        val currentLayout =
            layout ?: return null

        val (x, y) =
            toDiagramCoordinates(
                mouseX,
                mouseY
            )

        val mousePoint =
            Point(
                x,
                y
            )

        /*
         * If several edges overlap, choose
         * the edge whose segment is closest
         * to the mouse.
         */
        return currentGraph.edges
            .mapNotNull { edge ->

                val edgeLayout =
                    currentLayout.edges[edge.id]
                        ?: return@mapNotNull null

                val distance =
                    edgeLayout.points
                        .zipWithNext()
                        .minOfOrNull { (start, end) ->
                            distanceToSegment(
                                mousePoint,
                                start,
                                end
                            )
                        }
                        ?: return@mapNotNull null

                if (
                    distance <=
                    EDGE_CLICK_TOLERANCE
                ) {
                    EdgeDistance(
                        edge = edge,
                        distance = distance
                    )
                } else {
                    null
                }
            }
            .minByOrNull {
                it.distance
            }
            ?.edge
    }

    /**
     * Distance from a point to a line segment.
     *
     * Works for horizontal, vertical and also
     * non-orthogonal segments if such a segment
     * ever appears in the future.
     */
    private fun distanceToSegment(
        point: Point,
        start: Point,
        end: Point
    ): Double {

        val dx =
            (end.x - start.x)
                .toDouble()

        val dy =
            (end.y - start.y)
                .toDouble()

        /*
         * Zero-length segment.
         */
        if (
            dx == 0.0 &&
            dy == 0.0
        ) {

            val px =
                point.x - start.x

            val py =
                point.y - start.y

            return kotlin.math.sqrt(
                (
                        px * px +
                                py * py
                        ).toDouble()
            )
        }

        val lengthSquared =
            dx * dx +
                    dy * dy

        var t =
            (
                    (point.x - start.x) * dx +
                            (point.y - start.y) * dy
                    ) /
                    lengthSquared

        t =
            t.coerceIn(
                0.0,
                1.0
            )

        val projectionX =
            start.x +
                    t * dx

        val projectionY =
            start.y +
                    t * dy

        val distanceX =
            point.x -
                    projectionX

        val distanceY =
            point.y -
                    projectionY

        return kotlin.math.sqrt(
            distanceX * distanceX +
                    distanceY * distanceY
        )
    }

    // -------------------------------------------------------------------------
    // Existing hit testing
    // -------------------------------------------------------------------------

    private fun findActionAt(
        mouseX: Int,
        mouseY: Int
    ): ActionHitArea? {

        val (x, y) =
            toDiagramCoordinates(
                mouseX,
                mouseY
            )

        return hitRegistry
            .actionAreas
            .firstOrNull {
                it.bounds.contains(
                    x,
                    y
                )
            }
    }

    private fun findAction(
        hitArea: ActionHitArea
    ): SsmAction? =
        graph
            ?.edges
            ?.firstOrNull {
                it.id ==
                        hitArea.edgeId
            }
            ?.actions
            ?.getOrNull(
                hitArea.actionIndex
            )

    private fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? {

        val currentGraph =
            graph ?: return null

        val currentLayout =
            layout ?: return null

        val (x, y) =
            toDiagramCoordinates(
                mouseX,
                mouseY
            )

        return currentGraph.nodes
            .firstOrNull { node ->

                val nodeLayout =
                    currentLayout
                        .nodes[node.id]
                        ?: return@firstOrNull false

                x in
                        nodeLayout.x..
                        (nodeLayout.x +
                                nodeLayout.width) &&

                        y in
                        nodeLayout.y..
                        (nodeLayout.y +
                                nodeLayout.height)
            }
    }

    private fun findEventEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? =
        findEdgeAt(
            hitRegistry.eventAreas,
            mouseX,
            mouseY
        )

    private fun findGuardEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? =
        findEdgeAt(
            hitRegistry.guardAreas,
            mouseX,
            mouseY
        )

    private fun findEdgeAt(
        areas: Map<String, java.awt.Rectangle>,
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val currentGraph =
            graph ?: return null

        val (x, y) =
            toDiagramCoordinates(
                mouseX,
                mouseY
            )

        val edgeId =
            areas.entries
                .firstOrNull {
                    it.value.contains(
                        x,
                        y
                    )
                }
                ?.key
                ?: return null

        return currentGraph.edges
            .firstOrNull {
                it.id == edgeId
            }
    }

    // -------------------------------------------------------------------------
    // Coordinates
    // -------------------------------------------------------------------------

    private fun toDiagramCoordinates(
        mouseX: Int,
        mouseY: Int
    ): Pair<Int, Int> =
        Pair(
            (mouseX / zoom).toInt(),
            (mouseY / zoom).toInt()
        )

    // -------------------------------------------------------------------------
    // Zoom internals
    // -------------------------------------------------------------------------

    private fun setZoom(
        value: Double
    ) {

        val newZoom =
            value.coerceIn(
                MIN_ZOOM,
                MAX_ZOOM
            )

        if (
            zoom == newZoom
        ) {
            return
        }

        zoom =
            newZoom

        updatePreferredSize()

        revalidate()
        repaint()

        zoomChangedListener
            ?.invoke(zoom)
    }

    private fun updatePreferredSize() {

        val currentLayout =
            layout ?: return

        preferredSize =
            Dimension(
                (
                        currentLayout.size.width *
                                zoom
                        ).toInt(),
                (
                        currentLayout.size.height *
                                zoom
                        ).toInt()
            )
    }

    // -------------------------------------------------------------------------
    // Internal types
    // -------------------------------------------------------------------------

    private data class EdgeDistance(
        val edge: DiagramEdge,
        val distance: Double
    )

    private companion object {

        const val DEFAULT_ZOOM =
            1.0

        const val MIN_ZOOM =
            0.25

        const val MAX_ZOOM =
            2.5

        const val ZOOM_STEP =
            0.10

        const val FIT_PADDING =
            40

        /*
         * Invisible clickable area around an edge.
         *
         * 10 means approximately 20 px total
         * clickable width around the line.
         */
        const val EDGE_CLICK_TOLERANCE =
            10.0

        val HAND_CURSOR: Cursor =
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
    }
}