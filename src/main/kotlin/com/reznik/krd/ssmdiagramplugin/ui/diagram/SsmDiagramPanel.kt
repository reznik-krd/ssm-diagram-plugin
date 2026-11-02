package com.reznik.krd.ssmdiagramplugin.ui.diagram

import com.intellij.ui.JBColor
import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.builder.DiagramGraphBuilder
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionHitArea
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.ActionKey
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramZoomController
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramPanController
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramKeyboardHandler
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.HierarchicalLayoutEngine
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import com.reznik.krd.ssmdiagramplugin.ui.diagram.render.DiagramRenderer
import com.reznik.krd.ssmdiagramplugin.ui.diagram.transform.ReadableGraphTransformer
import com.intellij.openapi.project.Project
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramNavigator
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitTester
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramSelectionController
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
 * Swing-панель интерактивной диаграммы: отображение, zoom, hit testing, выбор и навигация.
 */
class SsmDiagramPanel(project: Project) : JPanel() {

    private val graphBuilder = DiagramGraphBuilder()
    private val graphTransformer = ReadableGraphTransformer()
    private val layoutEngine = HierarchicalLayoutEngine()
    private val interaction = DiagramInteractionState()
    private val hitRegistry = DiagramHitRegistry()
    private val navigator = DiagramNavigator(project)
    private val renderer = DiagramRenderer(interaction, hitRegistry)
    private val hitTester = DiagramHitTester({ graph }, { layout }, { zoom }, hitRegistry)
    private val selectionController = DiagramSelectionController(interaction) { repaint() }

    private var graph: DiagramGraph? = null
    private var layout: DiagramLayout? = null
    private val zoomController = DiagramZoomController(this) { layout }
    private val zoom: Double get() = zoomController.zoom
    private var displayOptions = DiagramDisplayOptions()
    private val panController = DiagramPanController(this) { layout != null }
    private val keyboardHandler = DiagramKeyboardHandler(this, panController) { layout != null }
    private val spacePressed: Boolean get() = panController.spacePressed
    private val panning: Boolean get() = panController.panning
    private var dragDistance: Int
        get() = panController.dragDistance
        set(value) { panController.dragDistance = value }

    init {
        background = JBColor.PanelBackground
        installMouseHandlers()
        isFocusable = true
    }

    // -------------------------------------------------------------------------
    // Zoom API
    // -------------------------------------------------------------------------

    /** Регистрирует слушателя изменения масштаба. */
    fun setZoomChangedListener(listener: (Double) -> Unit) = zoomController.setZoomChangedListener(listener)

    /** Увеличивает масштаб. */
    fun zoomIn() = zoomController.zoomIn()

    /** Уменьшает масштаб. */
    fun zoomOut() = zoomController.zoomOut()

    /** Возвращает масштаб 100%. */
    fun resetZoom() = zoomController.resetZoom()

    /** Вписывает диаграмму в заданный размер окна. */
    fun fitToViewport(viewportSize: Dimension) = zoomController.fitToViewport(viewportSize)

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    /**
     * Строит визуальный граф для модели и обновляет диаграмму.
     */
    fun setModel(
        model: StateMachineModel
    ) {
        val logicalGraph = graphBuilder.build(model)
        val visualGraph = graphTransformer.transform(logicalGraph)
        graph = visualGraph
        layout = layoutEngine.layout(graph = visualGraph, options = displayOptions)

        interaction.clear()
        hitRegistry.clear()

        zoomController.updatePreferredSize()

        revalidate()
        repaint()
    }

    /**
     * Сбрасывает текущее состояние объекта.
     */
    fun clear() {
        panController.reset()
        graph = null
        layout = null
        interaction.clear()
        hitRegistry.clear()
        zoomController.resetState()
        preferredSize = Dimension(0, 0)
        cursor = Cursor.getDefaultCursor()
        revalidate()
        repaint()
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    /**
     * Отрисовывает текущую диаграмму с учётом масштаба.
     */
    override fun paintComponent(
        graphics: Graphics
    ) {
        super.paintComponent(graphics)
        val currentGraph = graph ?: return
        val currentLayout = layout ?: return
        val g2 = graphics.create() as Graphics2D

        try {
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )

            g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )

            g2.scale(zoom, zoom)

            renderer.render(g2 = g2, graph = currentGraph, layout = currentLayout, options = displayOptions)
        } finally {
            g2.dispose()
        }
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    /**
     * Подключает обработчики мыши для zoom, hover и кликов.
     */
    private fun installMouseHandlers() {
        addMouseWheelListener(zoomController::handleWheel)
        addMouseListener(
            object : MouseAdapter() {

                /** Обрабатывает щелчок по элементу диаграммы. */
                override fun mouseClicked(event: MouseEvent) {
                    if (spacePressed || dragDistance > 4) {
                        dragDistance = 0
                        return
                    }

                    handleMouseClick(event)
                }

                /** Сбрасывает наведение при выходе указателя за диаграмму. */
                override fun mouseExited(
                    event: MouseEvent
                ) {
                    clearHover()
                }

                /** Начинает перемещение холста при нажатии мыши с пробелом. */
                override fun mousePressed(event: MouseEvent) {
                    requestFocusInWindow()
                    if (spacePressed) {
                        panController.startPanning(event)
                    }
                }

                /** Завершает перемещение холста после отпускания кнопки мыши. */
                override fun mouseReleased(event: MouseEvent) {
                    if (panning) {
                        panController.stopPanning()
                    }
                }
            }
        )

        addMouseMotionListener(
            object : MouseMotionAdapter() {

                /** Обновляет подсветку элемента под указателем. */
                override fun mouseMoved(event: MouseEvent) {
                    if (spacePressed || panning) {
                        return
                    }

                    handleMouseMove(event)
                }

                /** Перемещает холст во время перетаскивания. */
                override fun mouseDragged(event: MouseEvent) {
                    if (panning) {
                        panController.handlePanning(event)
                    }
                }
            }
        )
    }

    /**
     * Определяет интерактивный элемент под курсором и обновляет hover.
     */
    private fun handleMouseMove(
        event: MouseEvent
    ) {

        /*
         * 1. Action.
         */
        val actionArea = findActionAt(event.x, event.y)
        val action = actionArea?.let(::findAction)

        if (actionArea != null && action?.action?.targetClass != null) {
            updateHover(action = ActionKey(actionArea.edgeId, actionArea.actionIndex))
            return
        }

        /*
         * 2. Event.
         */
        val eventEdge = findEventEdgeAt(event.x, event.y)

        if (eventEdge != null && eventEdge.eventElement != null) {
            updateHover(eventEdgeId = eventEdge.id)
            return
        }

        /*
         * 3. Guard.
         */
        val guardEdge = findGuardEdgeAt(event.x, event.y)

        if (guardEdge != null && guardEdge.guard?.targetClass != null) {
            updateHover(guardEdgeId = guardEdge.id)
            return
        }

        /*
         * 4. State.
         */
        val node = findNodeAt(event.x, event.y)
                ?.takeIf {
                    it.state?.source != null
                }

        if (node != null) {
            updateHover(nodeId = node.id)
            return
        }

        /*
         * 5. Edge.
         *
         * Edge hover does not change selection,
         * but cursor tells the user it is clickable.
         */
        val edge = findDiagramEdgeAt(event.x, event.y)
        if (edge != null) {
            updateHover(clickable = true)
            return
        }
        updateHover(
            clickable = false
        )
    }

    /**
     * Обновляет состояние hover и курсор мыши.
     */
    private fun updateHover(
        nodeId: String? = null,
        eventEdgeId: String? = null,
        guardEdgeId: String? = null,
        action: ActionKey? = null,
        clickable: Boolean = true
    ) {
        if (spacePressed || panning) {
            return
        }

        val changed = interaction.hoveredNodeId != nodeId ||
                    interaction.hoveredEventEdgeId != eventEdgeId ||
                    interaction.hoveredGuardEdgeId != guardEdgeId ||
                    interaction.hoveredAction != action

        interaction.hoveredNodeId = nodeId

        interaction.hoveredEventEdgeId = eventEdgeId

        interaction.hoveredGuardEdgeId = guardEdgeId

        interaction.hoveredAction = action

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

    /**
     * Сбрасывает состояние hover.
     */
    private fun clearHover() {
        if (spacePressed || panning) {
            return
        }
        val changed = interaction.hasHover()
        interaction.clearHover()
        cursor = Cursor.getDefaultCursor()
        if (changed) {
            repaint()
        }
    }

    // -------------------------------------------------------------------------
    // Click
    // -------------------------------------------------------------------------

    /**
     * Обрабатывает навигацию и выбор элемента по клику.
     */
    private fun handleMouseClick(event: MouseEvent) {
        if (event.button != MouseEvent.BUTTON1) {
            return
        }

        /*
         * 1. Action navigation.
         */
        val actionClass = findActionAt(event.x, event.y)
                ?.let(::findAction)
                ?.action
                ?.targetClass

        if (actionClass != null) {
            navigator.navigateTo(actionClass)
            return
        }

        /*
         * 2. Event navigation.
         */
        val eventElement = findEventEdgeAt(event.x, event.y)
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
        val guardClass = findGuardEdgeAt(event.x, event.y)
                ?.guard
                ?.targetClass

        if (guardClass != null) {
            navigator.navigateTo(guardClass)
            return
        }

        /*
         * 4. State.
         *
         * State has priority over edge because
         * an edge can visually pass behind a node.
         */
        val node = findNodeAt(event.x, event.y)
        if (node != null && node.state?.source != null) {
            selectNode(node)

            /*
             * Existing behavior:
             * navigate directly to enum constant.
             */
            navigator.navigateTo(node.state.source)
            return
        }

        /*
         * 5. Edge selection.
         */
        val edge = findDiagramEdgeAt(event.x, event.y)
        if (edge != null) {
            selectEdge(edge)
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

    /**
     * Выбирает логическое состояние и связанные входящие переходы.
     */
    private fun selectNode(
        node: DiagramNode
    ) = selectionController.selectNode(node)

    /**
     * Выбирает конкретный переход диаграммы.
     */
    private fun selectEdge(
        edge: DiagramEdge
    ) = selectionController.selectEdge(edge)

    /**
     * Сбрасывает выбранное состояние или переход.
     */
    private fun clearSelection() = selectionController.clearSelection()

    // -------------------------------------------------------------------------
    // Hit testing
    // -------------------------------------------------------------------------

    /**
     * Находит узел диаграммы в координатах мыши.
     */
    private fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? = hitTester.findNodeAt(mouseX, mouseY)

    /**
     * Находит кликабельную область action в координатах мыши.
     */
    private fun findActionAt(
        mouseX: Int,
        mouseY: Int
    ): ActionHitArea? = hitTester.findActionAt(mouseX, mouseY)

    /**
     * Находит модель action по зарегистрированной hit-area.
     */
    private fun findAction(
        area: ActionHitArea
    ): SsmAction? = hitTester.findAction(area)

    /**
     * Находит переход по области подписи event.
     */
    private fun findEventEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? = hitTester.findEventEdgeAt(mouseX, mouseY)

    /**
     * Находит переход по области подписи guard.
     */
    private fun findGuardEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? = hitTester.findGuardEdgeAt(mouseX, mouseY)

    /**
     * Находит ребро рядом с указанной точкой диаграммы.
     */
    private fun findDiagramEdgeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramEdge? = hitTester.findDiagramEdgeAt(mouseX, mouseY)

    // -------------------------------------------------------------------------
    // Zoom
    // -------------------------------------------------------------------------

    /** Подключает обработчик клавиатуры при добавлении панели в UI. */
    override fun addNotify() {
        super.addNotify()
        keyboardHandler.attach()
    }

    /** Освобождает обработчик клавиатуры при удалении панели. */
    override fun removeNotify() {
        keyboardHandler.detach()
        super.removeNotify()
    }

    /**
     * Изменяет видимость подписей без повторного PSI-анализа.
     */
    fun setDisplayOptions(options: DiagramDisplayOptions) {
        if (displayOptions == options) {
            return
        }

        displayOptions = options
        val currentGraph = graph ?: return

        layout = layoutEngine.layout(graph = currentGraph, options = displayOptions)
        hitRegistry.clear()
        zoomController.updatePreferredSize()

        revalidate()
        repaint()
    }


    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private companion object {

        /*
         * Screen-space pixels.
         */

        val HAND_CURSOR: Cursor =
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
    }
}