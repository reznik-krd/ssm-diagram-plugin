package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/** Opens PSI elements referenced by interactive diagram elements in the editor. */
class DiagramNavigator(
    private val project: Project
) {
    /** Opens the given PSI element and moves the caret to its source offset. */
    fun navigateTo(element: PsiElement) {
        val virtualFile = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, virtualFile, element.textOffset).navigate(true)
    }
}
