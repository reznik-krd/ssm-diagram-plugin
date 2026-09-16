package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

data class SsmTransition(
    val source: String,
    val target: String,

    val event: String? = null,

    val action: SsmComponentReference? = null,
    val errorAction: SsmComponentReference? = null,
    val guard: SsmComponentReference? = null,

    val sourceElement: PsiElement? = null,
    val targetElement: PsiElement? = null,
    val eventElement: PsiElement? = null
)