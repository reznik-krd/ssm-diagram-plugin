package com.reznik.krd.ssmdiagramplugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.reznik.krd.ssmdiagramplugin.analyzer.SsmPsiAnalyzer
import com.reznik.krd.ssmdiagramplugin.ui.SsmDiagramService

class GenerateSsmDiagramAction : AnAction() {

    private val analyzer = SsmPsiAnalyzer()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: return

        val psiFile = PsiManager
            .getInstance(project)
            .findFile(virtualFile)

        if (psiFile !is PsiJavaFile) {
            Messages.showWarningDialog(
                project,
                "Selected file is not a Java file.",
                "Spring StateMachine Diagram"
            )
            return
        }

        val model = analyzer.analyze(psiFile)

        if (model == null) {
            Messages.showWarningDialog(
                project,
                "Spring StateMachine configuration was not found.",
                "Spring StateMachine Diagram"
            )
            return
        }

        val diagramService = SsmDiagramService.getInstance(project)
        diagramService.show(model)
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SSM Diagram")
        toolWindow?.show()
    }
}