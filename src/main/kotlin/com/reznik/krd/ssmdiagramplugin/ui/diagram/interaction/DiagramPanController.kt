package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.SwingUtilities

/** Управляет перемещением видимой области по Space + ЛКМ. */
internal class DiagramPanController(
    private val panel: JPanel,
    private val hasLayout: () -> Boolean
) {
    var spacePressed: Boolean = false
    var panning: Boolean = false
        private set
    var dragDistance: Int = 0
    private var lastDragScreenPoint: Point? = null
    private val panCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)

    /** Останавливает перемещение и сбрасывает состояние клавиатуры. */
    fun reset() {
        spacePressed = false
        stopPanning()
        dragDistance = 0
    }

    /** Запоминает положение мыши и начинает перемещение видимой области. */
    fun startPanning(event: MouseEvent) {
        if (!spacePressed || !hasLayout()) {
            return
        }

        if (event.button != MouseEvent.BUTTON1) {
            return
        }

        panning = true
        dragDistance = 0
        lastDragScreenPoint = event.locationOnScreen

        panel.cursor = panCursor
    }

    /** Смещает видимую область согласно движению мыши. */
    fun handlePanning(event: MouseEvent) {
        if (!panning) {
            return
        }

        val viewport = SwingUtilities.getAncestorOfClass(
            JViewport::class.java,
            panel
        ) as? JViewport ?: return

        val previousPoint = lastDragScreenPoint ?: return
        val currentPoint = event.locationOnScreen

        val dx = currentPoint.x - previousPoint.x
        val dy = currentPoint.y - previousPoint.y

        if (dx == 0 && dy == 0) {
            return
        }

        dragDistance += kotlin.math.abs(dx) + kotlin.math.abs(dy)

        val currentPosition = viewport.viewPosition
        val extent = viewport.extentSize
        val viewSize = viewport.viewSize

        val maxX = (viewSize.width - extent.width).coerceAtLeast(0)
        val maxY = (viewSize.height - extent.height).coerceAtLeast(0)

        viewport.viewPosition = Point(
            (currentPosition.x - dx).coerceIn(0, maxX),
            (currentPosition.y - dy).coerceIn(0, maxY)
        )

        lastDragScreenPoint = currentPoint
    }

    /** Завершает перемещение холста и восстанавливает курсор. */
    fun stopPanning() {
        panning = false
        lastDragScreenPoint = null

        panel.cursor = if (spacePressed) {
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        } else {
            Cursor.getDefaultCursor()
        }
    }

}
