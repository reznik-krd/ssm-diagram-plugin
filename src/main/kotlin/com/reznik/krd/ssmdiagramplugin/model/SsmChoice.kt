package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

/**
 * Semantic representation of a Spring StateMachine choice pseudo-state.
 */
data class SsmChoice(
    val source: String,
    val branches: List<SsmChoiceBranch>,
    val sourceElement: PsiElement? = null
)

data class SsmChoiceBranch(
    val target: String,
    val type: Type,

    val guard: SsmComponentReference? = null,
    val actions: List<SsmAction> = emptyList(),

    val targetElement: PsiElement? = null
) {

    enum class Type {
        FIRST,
        THEN,
        LAST
    }
}