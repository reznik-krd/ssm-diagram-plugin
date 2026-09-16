package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * Resolves supported PSI expressions to semantic state or event values.
 */
class ExpressionResolver {

    /** Resolves a supported PSI expression into one or more symbolic values. */
    fun resolve(expression: PsiExpression): List<ResolvedValue> {
        return when (expression) {
            is PsiReferenceExpression -> resolveReference(expression)

            else -> emptyList()
        }
    }

    private fun resolveReference(
        expression: PsiReferenceExpression
    ): List<ResolvedValue> {

        val resolved = expression.resolve()

        return when (resolved) {

            is PsiEnumConstant -> {
                listOf(
                    ResolvedValue(
                        name = resolved.name,
                        source = resolved
                    )
                )
            }

            is PsiLocalVariable -> {
                resolveLocalVariable(resolved)
            }

            is PsiParameter -> {
                resolveParameter(resolved)
            }

            else -> emptyList()
        }
    }

    private fun resolveLocalVariable(
        variable: PsiLocalVariable
    ): List<ResolvedValue> {

        val initializer = variable.initializer
            ?: return emptyList()

        return resolve(initializer)
    }

    private fun resolveParameter(
        parameter: PsiParameter
    ): List<ResolvedValue> {

        val foreach = PsiTreeUtil.getParentOfType(
            parameter,
            PsiForeachStatement::class.java
        ) ?: return emptyList()

        if (foreach.iterationParameter != parameter) {
            return emptyList()
        }

        val iteratedValue = foreach.iteratedValue
            ?: return emptyList()

        return resolveCollection(iteratedValue)
    }

    private fun resolveCollection(
        expression: PsiExpression
    ): List<ResolvedValue> {

        if (expression is PsiMethodCallExpression) {

            val methodName =
                expression.methodExpression.referenceName

            if (methodName == "of") {
                return expression.argumentList
                    .expressions
                    .flatMap(::resolve)
            }
        }

        if (expression is PsiReferenceExpression) {

            val resolved = expression.resolve()

            if (resolved is PsiLocalVariable) {
                val initializer = resolved.initializer
                    ?: return emptyList()

                return resolveCollection(initializer)
            }
        }

        return emptyList()
    }
}