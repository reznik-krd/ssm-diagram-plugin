package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Создаёт Tool Window SSM Diagram и подключает его основную панель.
 */
class SsmDiagramToolWindowFactory : ToolWindowFactory {

    /**
     * Определяет доступность Tool Window для открытого проекта.
     */
    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

    /**
     * Создаёт и добавляет содержимое Tool Window.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SsmDiagramToolWindowPanel(project)
        val content = ContentFactory
            .getInstance()
            .createContent(
                panel,
                "Diagram",
                false
            )

        toolWindow
            .contentManager
            .addContent(
                content
            )
    }
}