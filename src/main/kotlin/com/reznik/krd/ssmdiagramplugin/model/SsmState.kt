package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiElement

/**
 * Semantic state declared by a Spring StateMachine configuration.
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