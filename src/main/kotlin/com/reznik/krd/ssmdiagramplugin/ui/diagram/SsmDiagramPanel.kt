package com.reznik.krd.ssmdiagramplugin.ui.diagram

import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.builder.DiagramGraphBuilder
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionHitArea
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionKey
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.HierarchicalLayoutEngine
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import com.reznik.krd.ssmdiagramplugin.ui.diagram.render.DiagramRenderer
import com.reznik.krd.ssmdiagramplugin.ui.diagram.transform.ReadableGraphTransformer
import com.intellij.openapi.project.Project
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramNavigator
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel
import kotlin.math.sqrt

/**
 * Swing surface that displays an analyzed Spring StateMachine diagram.
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

    private var graph: DiagramGraph? =
        null

    private var layout: DiagramLayout? =
        null

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
    // Zoom API
    // -------------------------------------------------------------------------

    fun setZoomChangedListener(
        listener: (Double) -> Unit
    ) {
        zoomChangedListener =
            listener

        listener(zoom)
    }

    fun zoomIn() =
        setZoom(
            zoom + ZOOM_STEP
        )

    fun zoomOut() =
        setZoom(
            zoom - ZOOM_STEP
        )

    fun resetZoom() =
        setZoom(
            DEFAULT_ZOOM
        )

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
            (
                    viewportSize.width -
                            FIT_PADDING
                    )
                .coerceAtLeast(1)

        val availableHeight =
            (
                    viewportSize.height -
                            FIT_PADDING
                    )
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

    fun clear() {

        graph =
            null

        layout =
            null

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
    // Painting
    // -------------------------------------------------------------------------

    override fun paintComponent(
        graphics: Graphics
    ) {

        super.paintComponent(
            graphics
        )

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

            if (
                event.wheelRotation < 0
            ) {
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
                    handleMouseClick(
                        event
                    )
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
                    handleMouseMove(
                        event
                    )
                }
            }
        )
    }

    private fun handleMouseMove(
        event: MouseEvent
    ) {

        /*
         * 1. Action.
         */
        val actionArea =
            findActionAt(
                event.x,
                event.y
            )

        val action =
            actionArea
                ?.let(::findAction)

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
         * 2. Event.
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
         * 3. Guard.
         */
        val guardEdge =
            findGuardEdgeAt(
                event.x,
                event.y
            )

        if (
            guardEdge != null &&
            guardEdge.guard?.targetClass != null
        ) {

            updateHover(
                guardEdgeId =
                    guardEdge.id
            )

            return
        }

        /*
         * 4. State.
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
                nodeId =
                    node.id
            )

            return
        }

        /*
         * 5. Edge.
         *
         * Edge hover does not change selection,
         * but cursor tells the user it is clickable.
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
         * Important:
         *
         * mouse exit clears only hover.
         * Selected state/edge must remain selected.
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
         * 1. Action navigation.
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
         * 2. Event navigation.
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
         * 3. Guard navigation.
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
         * 4. State.
         *
         * State has priority over edge because
         * an edge can visually pass behind a node.
         */
        val node =
            findNodeAt(
                event.x,
                event.y
            )

        if (
            node != null &&
            node.state?.source != null
        ) {

            selectNode(
                node
            )

            /*
             * Existing behavior:
             * navigate directly to enum constant.
             */
            navigator.navigateTo(
                node.state.source
            )

            return
        }

        /*
         * 5. Edge selection.
         */
        val edge =
            findDiagramEdgeAt(
                event.x,
                event.y
            )

        if (edge != null) {

            selectEdge(
                edge
            )

            return
        }

        /*
         * Empty click clears current selection.
         */
        clearSelection()
    }

    // -------------------------------------------------------------------------
    // Selection
    // -------------------------------------------------------------------------

    private fun selectNode(
        node: DiagramNode
    ) {

        val logicalId =
            node.logicalId

        /*
         * Repeated click toggles selection off.
         */
        interaction.selectedNodeLogicalId =
            if (
                interaction.selectedNodeLogicalId ==
                logicalId
            ) {
                null
            } else {
                logicalId
            }

        /*
         * State selection and explicit edge selection
         * are mutually exclusive.
         */
        interaction.selectedEdgeId =
            null

        repaint()
    }

    private fun selectEdge(
        edge: DiagramEdge
    ) {

        /*
         * Repeated click toggles selection off.
         */
        interaction.selectedEdgeId =
            if (
                interaction.selectedEdgeId ==
                edge.id
            ) {
                null
            } else {
                edge.id
            }

        /*
         * Explicit transition selection disables
         * logical state selection.
         */
        interaction.selectedNodeLogicalId =
            null

        repaint()
    }

    private fun clearSelection() {

        val changed =
            interaction.selectedEdgeId != null ||
                    interaction.selectedNodeLogicalId != null

        interaction.selectedEdgeId =
            null

        interaction.selectedNodeLogicalId =
            null

        if (changed) {
            repaint()
        }
    }

    // -------------------------------------------------------------------------
    // Hit testing
    // -------------------------------------------------------------------------

    private fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? {

        val currentGraph =
            graph ?: return null

        val currentLayout =
            layout ?: return null

        val diagramX =
            (mouseX / zoom)
                .toInt()

        val diagramY =
            (mouseY / zoom)
                .toInt()

        return currentGraph.nodes
            .firstOrNull { node ->

                val nodeLayout =
                    currentLayout.nodes[
                        node.id
                    ]
                        ?: return@firstOrNull false

                diagramX >= nodeLayout.x &&
                        diagramX <=
                        nodeLayout.x +
                        nodeLayout.width &&
                        diagramY >= nodeLayout.y &&
                        diagramY <=
                        nodeLayout.y +
                        nodeLayout.height
            }
    }

    private fun findActionAt(
        mouseX: Int,
        mouseY: Int
    ): ActionHitArea? {

        val diagramX =
            (mouseX / zoom)
                .toInt()

        val diagramY =
            (mouseY / zoom)
                .toInt()

        return hitRegistry.actionAreas
            .lastOrNull { area ->
                area.bounds.contains(
                    diagramX,
                    diagramY
                )
            }
    }

    private fun findAction(
        area: ActionHitArea
    ): SsmAction? {

        val currentGraph =
            graph ?: return null

        val edge =
            currentGraph.edges
                .firstOrNull {
                    it.id ==
                            area.edgeId
                }
                ?: return null

        return edge.actions
            .getOrNull(
                area.actionIndex
            )
    }

    private fun findEventEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val currentGraph =
            graph ?: return null

        val diagramX =
            (mouseX / zoom)
                .toInt()

        val diagramY =
            (mouseY / zoom)
                .toInt()

        val edgeId =
            hitRegistry.eventAreas
                .entries
                .lastOrNull { (_, bounds) ->
                    bounds.contains(
                        diagramX,
                        diagramY
                    )
                }
                ?.key
                ?: return null

        return currentGraph.edges
            .firstOrNull {
                it.id == edgeId
            }
    }

    private fun findGuardEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val currentGraph =
            graph ?: return null

        val diagramX =
            (mouseX / zoom)
                .toInt()

        val diagramY =
            (mouseY / zoom)
                .toInt()

        val edgeId =
            hitRegistry.guardAreas
                .entries
                .lastOrNull { (_, bounds) ->
                    bounds.contains(
                        diagramX,
                        diagramY
                    )
                }
                ?.key
                ?: return null

        return currentGraph.edges
            .firstOrNull {
                it.id == edgeId
            }
    }

    private fun findDiagramEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {

        val currentGraph =
            graph ?: return null

        val currentLayout =
            layout ?: return null

        val diagramX =
            mouseX / zoom

        val diagramY =
            mouseY / zoom

        /*
         * Keep click tolerance visually similar
         * at every zoom level.
         */
        val tolerance =
            EDGE_CLICK_TOLERANCE /
                    zoom

        return currentGraph.edges
            .mapNotNull { edge ->

                val edgeLayout =
                    currentLayout.edges[
                        edge.id
                    ]
                        ?: return@mapNotNull null

                val distance =
                    edgeLayout.points
                        .zipWithNext()
                        .minOfOrNull {
                                (start, end) ->

                            distanceToSegment(
                                px = diagramX,
                                py = diagramY,
                                start = start,
                                end = end
                            )
                        }
                        ?: return@mapNotNull null

                edge to distance
            }
            .filter {
                    (_, distance) ->
                distance <= tolerance
            }
            .minByOrNull {
                    (_, distance) ->
                distance
            }
            ?.first
    }

    private fun distanceToSegment(
        px: Double,
        py: Double,
        start: Point,
        end: Point
    ): Double {

        val dx =
            (
                    end.x -
                            start.x
                    )
                .toDouble()

        val dy =
            (
                    end.y -
                            start.y
                    )
                .toDouble()

        if (
            dx == 0.0 &&
            dy == 0.0
        ) {

            val pointDx =
                px -
                        start.x

            val pointDy =
                py -
                        start.y

            return sqrt(
                pointDx *
                        pointDx +
                        pointDy *
                        pointDy
            )
        }

        val t =
            (
                    (
                            px -
                                    start.x
                            ) *
                            dx +
                            (
                                    py -
                                            start.y
                                    ) *
                            dy
                    ) /
                    (
                            dx *
                                    dx +
                                    dy *
                                    dy
                            )

        val clampedT =
            t.coerceIn(
                0.0,
                1.0
            )

        val closestX =
            start.x +
                    clampedT *
                    dx

        val closestY =
            start.y +
                    clampedT *
                    dy

        val distanceX =
            px -
                    closestX

        val distanceY =
            py -
                    closestY

        return sqrt(
            distanceX *
                    distanceX +
                    distanceY *
                    distanceY
        )
    }

    // -------------------------------------------------------------------------
    // Zoom
    // -------------------------------------------------------------------------

    private fun setZoom(
        value: Double
    ) {

        zoom =
            value.coerceIn(
                MIN_ZOOM,
                MAX_ZOOM
            )

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
                        )
                    .toInt(),

                (
                        currentLayout.size.height *
                                zoom
                        )
                    .toInt()
            )
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

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
         * Screen-space pixels.
         */
        const val EDGE_CLICK_TOLERANCE =
            10.0

        val HAND_CURSOR: Cursor =
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
    }
}