package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class SsmDiagramToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(
        project: Project
    ): Boolean {
        return true
    }

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {

        /*
         * Теперь ToolWindow содержит не непосредственно
         * JBScrollPane, а полноценную панель:
         *
         * ┌───────────────────────────────────┐
         * │ Configuration / Generate / Refresh│
         * ├───────────────────────────────────┤
         * │                                   │
         * │            Diagram                │
         * │                                   │
         * └───────────────────────────────────┘
         */
        val panel =
            SsmDiagramToolWindowPanel(
                project
            )

        val content =
            ContentFactory
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