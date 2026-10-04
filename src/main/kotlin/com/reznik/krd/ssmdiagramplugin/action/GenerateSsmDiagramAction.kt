package com.reznik.krd.ssmdiagramplugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.reznik.krd.ssmdiagramplugin.service.SsmBackgroundTaskService
import com.reznik.krd.ssmdiagramplugin.ui.SsmDiagramService

/**
 * Запускает построение диаграммы StateMachine из контекстного меню IntelliJ IDEA.
 */
class GenerateSsmDiagramAction : AnAction() {

    /**
     * Запускает анализ выбранного файла и показывает построенную диаграмму.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

        if (psiFile !is PsiJavaFile) {
            Messages.showWarningDialog(
                project,
                "Selected file is not a Java file.",
                DIALOG_TITLE
            )
            return
        }

        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.show()

        val backgroundTaskService = SsmBackgroundTaskService(project)
        backgroundTaskService.analyze(
            psiFile = psiFile,
            onSuccess = { model ->
                if (model == null) {
                    Messages.showWarningDialog(
                        project,
                        "Spring StateMachine configuration was not found.",
                        DIALOG_TITLE
                    )
                    return@analyze
                }
                SsmDiagramService.getInstance(project).show(model)
            },
            onError = { error ->
                Messages.showErrorDialog(
                    project,
                    buildString {
                        append("Failed to generate StateMachine diagram.")
                        error.message
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                append("\n\n")
                                append(it)
                            }
                    },
                    DIALOG_TITLE
                )
            }
        )
    }

    private companion object {
        const val TOOL_WINDOW_ID = "SSM Diagram"
        const val DIALOG_TITLE = "Spring StateMachine Diagram"
    }
}