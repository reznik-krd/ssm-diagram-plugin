package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

/**
 * Описывает choice-состояние и набор его условных ветвей.
 */
data class SsmChoice(
    val source: String,
    val branches: List<SsmChoiceBranch>,
    val sourceElement: PsiElement? = null
)

/**
 * Описывает одну ветвь choice-перехода с целью, guard и actions.
 */
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