package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

data class SsmChoice(
    val source: String,
    val branches: List<SsmChoiceBranch>,
    val sourceElement: PsiElement? = null
)

data class SsmChoiceBranch(
    val target: String,
    val type: Type,
    val guard: SsmComponentReference? = null,
    val targetElement: PsiElement? = null
) {

    enum class Type {
        FIRST,
        THEN,
        LAST
    }
}