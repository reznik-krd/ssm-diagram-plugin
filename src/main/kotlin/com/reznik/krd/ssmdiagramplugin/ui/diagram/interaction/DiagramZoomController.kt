package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseWheelEvent
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.SwingUtilities
import kotlin.math.pow

/** Управляет масштабом диаграммы, размером холста и zoom относительно курсора. */
internal class DiagramZoomController(
    private val panel: JPanel,
    private val layoutProvider: () -> DiagramLayout?
) {
    var zoom: Double = DiagramInteractionConstants.DEFAULT_ZOOM
        private set
    private var zoomChangedListener: ((Double) -> Unit)? = null

    /**
     * Регистрирует обработчик изменения масштаба.
     */
    fun setZoomChangedListener(listener: (Double) -> Unit) {
        zoomChangedListener = listener
        listener(zoom)
    }

    /**
     * Увеличивает масштаб диаграммы.
     */
    fun zoomIn() = setZoom(zoom + DiagramInteractionConstants.ZOOM_STEP)

    /**
     * Уменьшает масштаб диаграммы.
     */
    fun zoomOut() = setZoom(zoom - DiagramInteractionConstants.ZOOM_STEP)

    /**
     * Возвращает масштаб диаграммы к 100%.
     */
    fun resetZoom() = setZoom(DiagramInteractionConstants.DEFAULT_ZOOM)

    /**
     * Подбирает масштаб диаграммы под размер viewport.
     */
    fun fitToViewport(viewportSize: Dimension) {
        val currentLayout = layoutProvider() ?: return

        if (
            currentLayout.size.width <= 0 ||
            currentLayout.size.height <= 0 ||
            viewportSize.width <= 0 ||
            viewportSize.height <= 0
        ) {
            return
        }

        val availableWidth = (viewportSize.width - DiagramInteractionConstants.FIT_PADDING).coerceAtLeast(1)

        val availableHeight = (viewportSize.height - DiagramInteractionConstants.FIT_PADDING)
                .coerceAtLeast(1)

        val widthZoom = availableWidth.toDouble() / currentLayout.size.width
        val heightZoom = availableHeight.toDouble() / currentLayout.size.height

        setZoom(minOf(widthZoom, heightZoom))
    }

    /** Изменяет масштаб колесом и сохраняет положение точки под курсором. */
    fun handleWheel(event: MouseWheelEvent) {
        if (layoutProvider() == null || event.preciseWheelRotation == 0.0) return
        event.consume()

        val viewport = SwingUtilities.getAncestorOfClass(
            JViewport::class.java, panel
        ) as? JViewport
        val previousZoom = zoom
        val multiplier = (1.0 + DiagramInteractionConstants.ZOOM_STEP)
            .pow(-event.preciseWheelRotation)
        val requestedZoom = (zoom * multiplier).coerceIn(
            DiagramInteractionConstants.MIN_ZOOM,
            DiagramInteractionConstants.MAX_ZOOM
        )
        if (requestedZoom == previousZoom) return

        val anchor = event.point
        val previousPosition = viewport?.viewPosition ?: Point(0, 0)
        setZoom(requestedZoom)

        if (viewport != null) {
            val ratio = zoom / previousZoom
            val newX = (anchor.x * ratio - (anchor.x - previousPosition.x)).toInt()
            val newY = (anchor.y * ratio - (anchor.y - previousPosition.y)).toInt()
            val extent = viewport.extentSize
            val size = panel.preferredSize
            viewport.viewPosition = Point(
                newX.coerceIn(0, (size.width - extent.width).coerceAtLeast(0)),
                newY.coerceIn(0, (size.height - extent.height).coerceAtLeast(0))
            )
        }
    }

    /**
     * Устанавливает масштаб в допустимых пределах.
     */
    private fun setZoom(value: Double) {
        zoom = value.coerceIn(DiagramInteractionConstants.MIN_ZOOM, DiagramInteractionConstants.MAX_ZOOM)
        updatePreferredSize()
        panel.revalidate()
        panel.repaint()
        zoomChangedListener?.invoke(zoom)
    }

    /**
     * Пересчитывает размер Swing-панели с учётом масштаба.
     */
    fun updatePreferredSize() {
        val currentLayout = layoutProvider() ?: return

        panel.preferredSize =
            Dimension(
                (currentLayout.size.width * zoom).toInt(),
                (currentLayout.size.height * zoom).toInt()
            )
    }

    /** Сбрасывает масштаб при очистке диаграммы. */
    fun resetState() {
        zoom = DiagramInteractionConstants.DEFAULT_ZOOM
        zoomChangedListener?.invoke(zoom)
    }
}
