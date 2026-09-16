package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory

class SsmDiagramToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {

        val diagramPanel =   SsmDiagramPanel(project)

        val scrollPane = JBScrollPane(diagramPanel)

        val content = ContentFactory
            .getInstance()
            .createContent(
                scrollPane,
                "Diagram",
                false
            )

        toolWindow.contentManager.addContent(content)

        SsmDiagramService
            .getInstance(project)
            .addListener { model ->

                diagramPanel.setModel(model)
            }
    }
}