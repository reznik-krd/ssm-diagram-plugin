package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.*
import com.reznik.krd.ssmdiagramplugin.model.SsmComponentReference

/**
 * Разрешает PSI-выражение в ссылку на компонент Spring, используемый как guard или action.
 */
class ComponentReferenceResolver {

    /**
     * Разрешает PSI-выражение в модельное значение или ссылку.
     */
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

    /**
     * Разрешает ссылочное PSI-выражение.
     */
    private fun resolveReference(expression: PsiReferenceExpression): SsmComponentReference {
        val resolved = expression.resolve()

        return when (resolved) {
            is PsiField -> resolveVariable(expression, resolved)
            is PsiLocalVariable -> resolveVariable(expression, resolved)
            is PsiParameter -> resolveVariable(expression, resolved)
            else ->
                SsmComponentReference(
                    name = expression.referenceName ?: expression.text,
                    className = null,
                    qualifiedName = null,
                    sourceElement = expression,
                    targetClass = null
                )
        }
    }

    /**
     * Разрешает ссылку на переменную через её тип или инициализатор.
     */
    private fun resolveVariable(expression: PsiReferenceExpression, variable: PsiVariable): SsmComponentReference {
        val psiClass = resolveClass(variable.type)

        return SsmComponentReference(
            name = variable.name,
            className = psiClass?.name,
            qualifiedName = psiClass?.qualifiedName,
            sourceElement = expression,
            targetClass = psiClass
        )
    }

    /**
     * Разрешает PSI-тип или выражение до соответствующего класса.
     */
    private fun resolveClass(type: PsiType): PsiClass? {
        return when (type) {
            is PsiClassType -> type.resolve()
            else -> null
        }
    }
}