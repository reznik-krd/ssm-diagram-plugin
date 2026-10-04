package com.reznik.krd.ssmdiagramplugin.analyzer.psi

import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression

/**
 * Собирает связанную цепочку fluent-вызовов Spring StateMachine DSL.
 */
internal fun collectFluentChain(start: PsiMethodCallExpression): List<PsiMethodCallExpression> {
    val result = mutableListOf<PsiMethodCallExpression>()
    var current: PsiMethodCallExpression? = start

    while (current != null) {
        if (current.methodExpression.referenceName == "and") break
        result += current

        val parentReference = current.parent as? PsiReferenceExpression ?: break
        current = parentReference.parent as? PsiMethodCallExpression
    }

    return result
}

/**
 * Находит вызов с указанным именем внутри fluent-цепочки.
 */
internal fun List<PsiMethodCallExpression>.findCall(name: String): PsiMethodCallExpression? =
    firstOrNull { it.methodExpression.referenceName == name }
