package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

/**
 * Описывает состояние StateMachine, его тип и PSI-источник.
 */
data class SsmState(
    val name: String,
    val type: Type,
    val source: PsiElement? = null
) {
    enum class Type {
        INITIAL,
        NORMAL,
        CHOICE,
        END
    }
}