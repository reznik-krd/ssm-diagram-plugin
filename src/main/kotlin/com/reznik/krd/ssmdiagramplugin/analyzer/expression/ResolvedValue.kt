package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.PsiElement

/**
 * Resolved symbolic value together with the PSI element that produced it.
 */
data class ResolvedValue(
    val name: String,
    val source: PsiElement
)