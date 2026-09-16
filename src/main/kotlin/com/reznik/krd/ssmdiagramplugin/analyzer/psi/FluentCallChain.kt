package com.reznik.krd.ssmdiagramplugin.analyzer.psi

import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression

/**
 * Collects one Spring fluent configuration chain and stops at `.and()` boundary.
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

/** Returns the first call with the requested method name from a fluent chain. */
internal fun List<PsiMethodCallExpression>.findCall(name: String): PsiMethodCallExpression? =
    firstOrNull { it.methodExpression.referenceName == name }
