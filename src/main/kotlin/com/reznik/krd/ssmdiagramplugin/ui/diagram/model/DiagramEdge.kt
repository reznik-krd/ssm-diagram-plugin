package com.reznik.krd.ssmdiagramplugin.ui.diagram.model

import com.intellij.psi.PsiElement
import com.reznik.krd.ssmdiagramplugin.model.SsmComponentReference

data class DiagramEdge(
    val id: String,

    val source: String,
    val target: String,
    val type: Type,

    val event: String? = null,
    val eventElement: PsiElement? = null,
    val guard: SsmComponentReference? = null,
    val action: SsmComponentReference? = null,
    val errorAction: SsmComponentReference? = null
) {

    enum class Type {
        TRANSITION,
        CHOICE_BRANCH,
        INITIAL,
        END
    }
}