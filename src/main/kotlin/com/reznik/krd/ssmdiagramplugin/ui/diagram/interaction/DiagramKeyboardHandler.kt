package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import java.awt.Cursor
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/** Перехватывает Space над диаграммой, не позволяя вводить пробел в редакторе IDEA. */
internal class DiagramKeyboardHandler(
    private val panel: JPanel,
    private val pan: DiagramPanController,
    private val hasLayout: () -> Boolean
) {
    private val dispatcher = KeyEventDispatcher { event ->
        val isSpaceEvent = when (event.id) {
            KeyEvent.KEY_TYPED -> event.keyChar == ' '
            else -> event.keyCode == KeyEvent.VK_SPACE
        }
        if (!isSpaceEvent) return@KeyEventDispatcher false

        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val window = SwingUtilities.getWindowAncestor(panel)
        if (window == null || window != manager.activeWindow) return@KeyEventDispatcher false

        when (event.id) {
            KeyEvent.KEY_PRESSED -> {
                if (pan.spacePressed) return@KeyEventDispatcher true
                if (panel.mousePosition == null || !hasLayout()) return@KeyEventDispatcher false
                panel.requestFocusInWindow()
                pan.spacePressed = true
                panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                true
            }
            KeyEvent.KEY_TYPED -> pan.spacePressed && event.keyChar == ' '
            KeyEvent.KEY_RELEASED -> {
                if (!pan.spacePressed) return@KeyEventDispatcher false
                pan.spacePressed = false
                pan.stopPanning()
                panel.cursor = Cursor.getDefaultCursor()
                true
            }
            else -> false
        }
    }

    /** Регистрирует обработчик только на время отображения панели. */
    fun attach() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
    }

    /** Удаляет глобальный обработчик и завершает перемещение. */
    fun detach() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
        pan.reset()
    }
}
