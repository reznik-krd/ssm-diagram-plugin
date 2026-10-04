package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.PsiElement

/**
 * Хранит разрешённое значение enum и PSI-элемент для последующей навигации в исходный код.
 */
data class ResolvedValue(
    val name: String,
    val source: PsiElement
)