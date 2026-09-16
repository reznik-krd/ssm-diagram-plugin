package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ComponentReferenceResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ExpressionResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ResolvedValue
import com.reznik.krd.ssmdiagramplugin.model.SsmTransition

class SsmTransitionAnalyzer {

    private val expressionResolver = ExpressionResolver()
    private val componentResolver = ComponentReferenceResolver()

    fun analyze(configClass: PsiClass): List<SsmTransition> {

        val transitions = mutableListOf<SsmTransition>()

        val calls = PsiTreeUtil.findChildrenOfType(
            configClass,
            PsiMethodCallExpression::class.java
        )

        calls
            .filter {
                it.methodExpression.referenceName == "withExternal"
            }
            .forEach { withExternal ->

                transitions += parseExternalTransition(
                    withExternal
                )
            }

        return transitions
    }

    private fun parseExternalTransition(
        withExternal: PsiMethodCallExpression
    ): List<SsmTransition> {

        val chain = collectChain(withExternal)

        val sourceCall =
            chain.findCall("source") ?: return emptyList()

        val targetCall =
            chain.findCall("target") ?: return emptyList()

        val sourceExpression =
            sourceCall.argumentList.expressions.firstOrNull()
                ?: return emptyList()

        val targetExpression =
            targetCall.argumentList.expressions.firstOrNull()
                ?: return emptyList()

        val sources = resolve(sourceExpression)
        val targets = resolve(targetExpression)

        if (sources.isEmpty() || targets.isEmpty()) {
            return emptyList()
        }

        val eventExpression =
            chain.findCall("event")
                ?.argumentList
                ?.expressions
                ?.firstOrNull()

        val guardExpression =
            chain.findCall("guard")
                ?.argumentList
                ?.expressions
                ?.firstOrNull()

        val actionExpressions =
            chain.findCall("action")
                ?.argumentList
                ?.expressions
                .orEmpty()

        val actionExpression =
            actionExpressions.getOrNull(0)

        val errorActionExpression =
            actionExpressions.getOrNull(1)

        val event = eventExpression?.let(::resolveSingleName)

        val guard =
            guardExpression?.let(componentResolver::resolve)

        val action =
            actionExpression?.let(componentResolver::resolve)

        val errorAction =
            errorActionExpression?.let(componentResolver::resolve)

        /*
         * Cartesian product.
         *
         * Обычно source и target содержат по одному значению.
         *
         * Но source может быть:
         *
         * [CREATED, CHECKING, READY]
         *
         * Тогда создаём три реальных перехода.
         */

        return sources.flatMap { source ->

            targets.map { target ->

                SsmTransition(
                    source = source.name,
                    target = target.name,

                    event = event,
                    guard = guard,
                    action = action,
                    errorAction = errorAction,

                    sourceElement = source.source,
                    targetElement = target.source,
                    eventElement = eventExpression
                )
            }
        }
    }

    private fun resolve(
        expression: PsiExpression
    ): List<ResolvedValue> {

        return expressionResolver.resolve(expression)
    }

    private fun resolveSingleName(
        expression: PsiExpression
    ): String? {

        return expressionResolver
            .resolve(expression)
            .firstOrNull()
            ?.name
            ?: resolveReferenceName(expression)
    }

    private fun resolveReferenceName(
        expression: PsiExpression
    ): String? {

        return when (expression) {

            is PsiReferenceExpression ->
                expression.referenceName

            else ->
                expression.text
        }
    }

    private fun collectChain(
        start: PsiMethodCallExpression
    ): List<PsiMethodCallExpression> {

        val result =
            mutableListOf<PsiMethodCallExpression>()

        var current: PsiMethodCallExpression? = start

        while (current != null) {

            result += current

            val parentReference =
                current.parent as? PsiReferenceExpression
                    ?: break

            current =
                parentReference.parent
                        as? PsiMethodCallExpression
        }

        return result
    }

    private fun List<PsiMethodCallExpression>.findCall(
        name: String
    ): PsiMethodCallExpression? {

        return firstOrNull {
            it.methodExpression.referenceName == name
        }
    }
}