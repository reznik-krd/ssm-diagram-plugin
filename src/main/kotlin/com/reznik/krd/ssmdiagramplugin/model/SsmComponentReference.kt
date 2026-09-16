package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

data class SsmComponentReference(
    val name: @NlsSafe String?,
    val className: String?,
    val qualifiedName: String?,
    val sourceElement: PsiElement?,
    val targetClass: PsiClass?
)