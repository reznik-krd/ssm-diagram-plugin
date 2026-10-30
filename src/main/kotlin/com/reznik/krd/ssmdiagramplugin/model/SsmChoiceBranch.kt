package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

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
    /** Допустимые разновидности элемента модели. */
    enum class Type {
        FIRST,
        THEN,
        LAST
    }
}
