package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.PsiElement

data class ResolvedValue(
    val name: String,
    val source: PsiElement
)