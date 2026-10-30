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
