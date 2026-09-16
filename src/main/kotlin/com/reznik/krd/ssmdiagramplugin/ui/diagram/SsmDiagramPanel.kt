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
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel

/**
 * Swing surface that displays an analyzed Spring StateMachine diagram.
 *
 * The panel coordinates graph preparation, zoom, mouse interaction and rendering while
 * delegating drawing, hit-area storage and source navigation to focused collaborators.
 */
class SsmDiagramPanel(project: Project) : JPanel() {

    private val graphBuilder = DiagramGraphBuilder()
    private val graphTransformer = ReadableGraphTransformer()
    private val layoutEngine = HierarchicalLayoutEngine()

    private val interaction = DiagramInteractionState()
    private val hitRegistry = DiagramHitRegistry()
    private val navigator = DiagramNavigator(project)
    private val renderer = DiagramRenderer(interaction, hitRegistry)

    private var graph: DiagramGraph? = null
    private var layout: DiagramLayout? = null

    private var zoom = DEFAULT_ZOOM
    private var zoomChangedListener: ((Double) -> Unit)? = null

    init {
        background = JBColor.PanelBackground
        installMouseHandlers()
    }

    /** Registers a listener invoked whenever the diagram zoom changes. */
    fun setZoomChangedListener(listener: (Double) -> Unit) {
        zoomChangedListener = listener
        listener(zoom)
    }

    /** Increases the diagram scale by one zoom step. */
    fun zoomIn() = setZoom(zoom + ZOOM_STEP)

    /** Decreases the diagram scale by one zoom step. */
    fun zoomOut() = setZoom(zoom - ZOOM_STEP)

    /** Restores the diagram to 100% scale. */
    fun resetZoom() = setZoom(DEFAULT_ZOOM)

    /** Fits the complete diagram into the supplied viewport while preserving aspect ratio. */
    fun fitToViewport(viewportSize: Dimension) {
        val currentLayout = layout ?: return
        if (currentLayout.size.width <= 0 || currentLayout.size.height <= 0 ||
            viewportSize.width <= 0 || viewportSize.height <= 0
        ) return

        val availableWidth = (viewportSize.width - FIT_PADDING).coerceAtLeast(1)
        val availableHeight = (viewportSize.height - FIT_PADDING).coerceAtLeast(1)
        val widthZoom = availableWidth.toDouble() / currentLayout.size.width
        val heightZoom = availableHeight.toDouble() / currentLayout.size.height
        setZoom(minOf(widthZoom, heightZoom))
    }

    /** Builds and displays a visual graph for the supplied analyzed state machine. */
    fun setModel(model: StateMachineModel) {
        val logicalGraph = graphBuilder.build(model)
        val visualGraph = graphTransformer.transform(logicalGraph)

        graph = visualGraph
        layout = layoutEngine.layout(visualGraph)
        interaction.clear()
        hitRegistry.clear()

        updatePreferredSize()
        revalidate()
        repaint()
    }

    /** Removes the current diagram and restores the default view state. */
    fun clear() {
        graph = null
        layout = null
        interaction.clear()
        hitRegistry.clear()
        zoom = DEFAULT_ZOOM
        zoomChangedListener?.invoke(zoom)
        preferredSize = Dimension(0, 0)
        cursor = Cursor.getDefaultCursor()
        revalidate()
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val currentGraph = graph ?: return
        val currentLayout = layout ?: return
        val g2 = graphics.create() as Graphics2D

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.scale(zoom, zoom)
            renderer.render(g2, currentGraph, currentLayout)
        } finally {
            g2.dispose()
        }
    }

    private fun installMouseHandlers() {
        addMouseWheelListener { event ->
            if (!event.isControlDown && !event.isMetaDown) return@addMouseWheelListener
            event.consume()
            if (event.wheelRotation < 0) zoomIn() else zoomOut()
        }

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = handleMouseClick(event)
            override fun mouseExited(event: MouseEvent) = clearHover()
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) = handleMouseMove(event)
        })
    }

    private fun handleMouseMove(event: MouseEvent) {
        val actionArea = findActionAt(event.x, event.y)
        val action = actionArea?.let(::findAction)
        if (actionArea != null && action?.action?.targetClass != null) {
            updateHover(action = ActionKey(actionArea.edgeId, actionArea.actionIndex))
            return
        }

        val eventEdge = findEventEdgeAt(event.x, event.y)
        if (eventEdge != null && eventEdge.eventElement != null) {
            updateHover(eventEdgeId = eventEdge.id)
            return
        }

        val guardEdge = findGuardEdgeAt(event.x, event.y)
        if (guardEdge != null && guardEdge.guard?.targetClass != null) {
            updateHover(guardEdgeId = guardEdge.id)
            return
        }

        val node = findNodeAt(event.x, event.y)?.takeIf { it.state?.source != null }
        updateHover(nodeId = node?.id, clickable = node != null)
    }

    private fun updateHover(
        nodeId: String? = null,
        eventEdgeId: String? = null,
        guardEdgeId: String? = null,
        action: ActionKey? = null,
        clickable: Boolean = true
    ) {
        val changed = interaction.hoveredNodeId != nodeId ||
            interaction.hoveredEventEdgeId != eventEdgeId ||
            interaction.hoveredGuardEdgeId != guardEdgeId ||
            interaction.hoveredAction != action

        interaction.hoveredNodeId = nodeId
        interaction.hoveredEventEdgeId = eventEdgeId
        interaction.hoveredGuardEdgeId = guardEdgeId
        interaction.hoveredAction = action
        cursor = if (clickable) HAND_CURSOR else Cursor.getDefaultCursor()

        if (changed) repaint()
    }

    private fun clearHover() {
        val changed = interaction.hasHover()
        interaction.clear()
        cursor = Cursor.getDefaultCursor()
        if (changed) repaint()
    }

    private fun handleMouseClick(event: MouseEvent) {
        if (event.button != MouseEvent.BUTTON1) return

        val actionClass = findActionAt(event.x, event.y)
            ?.let(::findAction)
            ?.action
            ?.targetClass
        if (actionClass != null) {
            navigator.navigateTo(actionClass)
            return
        }

        val eventElement = findEventEdgeAt(event.x, event.y)?.eventElement
        if (eventElement != null) {
            navigator.navigateTo(eventElement)
            return
        }

        val guardClass = findGuardEdgeAt(event.x, event.y)?.guard?.targetClass
        if (guardClass != null) {
            navigator.navigateTo(guardClass)
            return
        }

        findNodeAt(event.x, event.y)?.state?.source?.let(navigator::navigateTo)
    }

    private fun findActionAt(mouseX: Int, mouseY: Int): ActionHitArea? {
        val (x, y) = toDiagramCoordinates(mouseX, mouseY)
        return hitRegistry.actionAreas.firstOrNull { it.bounds.contains(x, y) }
    }

    private fun findAction(hitArea: ActionHitArea): SsmAction? =
        graph?.edges
            ?.firstOrNull { it.id == hitArea.edgeId }
            ?.actions
            ?.getOrNull(hitArea.actionIndex)

    private fun findNodeAt(mouseX: Int, mouseY: Int): DiagramNode? {
        val currentGraph = graph ?: return null
        val currentLayout = layout ?: return null
        val (x, y) = toDiagramCoordinates(mouseX, mouseY)

        return currentGraph.nodes.firstOrNull { node ->
            val nodeLayout = currentLayout.nodes[node.id] ?: return@firstOrNull false
            x in nodeLayout.x..(nodeLayout.x + nodeLayout.width) &&
                y in nodeLayout.y..(nodeLayout.y + nodeLayout.height)
        }
    }

    private fun findEventEdgeAt(mouseX: Int, mouseY: Int): DiagramEdge? =
        findEdgeAt(hitRegistry.eventAreas, mouseX, mouseY)

    private fun findGuardEdgeAt(mouseX: Int, mouseY: Int): DiagramEdge? =
        findEdgeAt(hitRegistry.guardAreas, mouseX, mouseY)

    private fun findEdgeAt(
        areas: Map<String, java.awt.Rectangle>,
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? {
        val currentGraph = graph ?: return null
        val (x, y) = toDiagramCoordinates(mouseX, mouseY)
        val edgeId = areas.entries.firstOrNull { it.value.contains(x, y) }?.key ?: return null
        return currentGraph.edges.firstOrNull { it.id == edgeId }
    }

    private fun toDiagramCoordinates(mouseX: Int, mouseY: Int): Pair<Int, Int> =
        Pair((mouseX / zoom).toInt(), (mouseY / zoom).toInt())

    private fun setZoom(value: Double) {
        val newZoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (zoom == newZoom) return

        zoom = newZoom
        updatePreferredSize()
        revalidate()
        repaint()
        zoomChangedListener?.invoke(zoom)
    }

    private fun updatePreferredSize() {
        val currentLayout = layout ?: return
        preferredSize = Dimension(
            (currentLayout.size.width * zoom).toInt(),
            (currentLayout.size.height * zoom).toInt()
        )
    }

    private companion object {
        const val DEFAULT_ZOOM = 1.0
        const val MIN_ZOOM = 0.25
        const val MAX_ZOOM = 2.5
        const val ZOOM_STEP = 0.10
        const val FIT_PADDING = 40

        val HAND_CURSOR: Cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
}
