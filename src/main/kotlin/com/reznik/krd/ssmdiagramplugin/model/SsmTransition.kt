package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

/**
 * Описывает переход между состояниями вместе с event, guard и actions.
 */
data class SsmTransition(
    val source: String,
    val target: String,
    val event: String? = null,
    val guard: SsmComponentReference? = null,
    val actions: List<SsmAction> = emptyList(),
    val sourceElement: PsiElement? = null,
    val targetElement: PsiElement? = null,
    val eventElement: PsiElement? = null
)