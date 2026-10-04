package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

/**
 * Хранит отображаемое имя компонента и PSI-ссылку на его класс.
 */
data class SsmComponentReference(
    val name: @NlsSafe String?,
    val className: String?,
    val qualifiedName: String?,
    val sourceElement: PsiElement?,
    val targetClass: PsiClass?
)