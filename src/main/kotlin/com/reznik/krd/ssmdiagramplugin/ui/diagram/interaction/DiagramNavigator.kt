package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * Открывает PSI-элементы диаграммы в редакторе IntelliJ IDEA.
 */
class DiagramNavigator(
    private val project: Project
) {

    /**
     * Открывает PSI-элемент в редакторе и переводит курсор к его объявлению.
     */
    fun navigateTo(element: PsiElement) {
        val virtualFile = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, virtualFile, element.textOffset).navigate(true)
    }
}
