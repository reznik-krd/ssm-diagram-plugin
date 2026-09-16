package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.*
import com.reznik.krd.ssmdiagramplugin.model.SsmComponentReference

/**
 * Resolves action and guard PSI expressions to navigable component references.
 */
class ComponentReferenceResolver {

    /** Resolves an action or guard expression into a navigable component reference. */
    fun resolve(expression: PsiExpression): SsmComponentReference {

        if (expression is PsiReferenceExpression) {
            return resolveReference(expression)
        }

        return SsmComponentReference(
            name = expression.text,
            className = null,
            qualifiedName = null,
            sourceElement = expression,
            targetClass = null
        )
    }

    private fun resolveReference(
        expression: PsiReferenceExpression
    ): SsmComponentReference {

        val resolved = expression.resolve()

        return when (resolved) {

            is PsiField ->
                resolveVariable(
                    expression,
                    resolved
                )

            is PsiLocalVariable ->
                resolveVariable(
                    expression,
                    resolved
                )

            is PsiParameter ->
                resolveVariable(
                    expression,
                    resolved
                )

            else ->
                SsmComponentReference(
                    name = expression.referenceName
                        ?: expression.text,
                    className = null,
                    qualifiedName = null,
                    sourceElement = expression,
                    targetClass = null
                )
        }
    }

    private fun resolveVariable(
        expression: PsiReferenceExpression,
        variable: PsiVariable
    ): SsmComponentReference {

        val psiClass = resolveClass(variable.type)

        return SsmComponentReference(
            name = variable.name,
            className = psiClass?.name,
            qualifiedName = psiClass?.qualifiedName,
            sourceElement = expression,
            targetClass = psiClass
        )
    }

    private fun resolveClass(
        type: PsiType
    ): PsiClass? {

        return when (type) {

            is PsiClassType ->
                type.resolve()

            else ->
                null
        }
    }
}