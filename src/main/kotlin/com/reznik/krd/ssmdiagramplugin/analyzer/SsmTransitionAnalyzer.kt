package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.reznik.krd.ssmdiagramplugin.analyzer.psi.collectFluentChain
import com.reznik.krd.ssmdiagramplugin.analyzer.psi.findCall
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ComponentReferenceResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ExpressionResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ResolvedValue
import com.reznik.krd.ssmdiagramplugin.model.SsmTransition
import com.reznik.krd.ssmdiagramplugin.model.SsmAction

/**
 * Extracts external transitions, events, guards and actions from a StateMachine configuration.
 */
class SsmTransitionAnalyzer {

    private val expressionResolver =
        ExpressionResolver()

    private val componentResolver =
        ComponentReferenceResolver()

    /** Extracts all external transitions declared by the configuration class. */
    fun analyze(
        configClass: PsiClass
    ): List<SsmTransition> {

        val transitions =
            mutableListOf<SsmTransition>()

        val calls =
            PsiTreeUtil.findChildrenOfType(
                configClass,
                PsiMethodCallExpression::class.java
            )

        calls
            .filter {
                it.methodExpression.referenceName ==
                        "withExternal"
            }
            .forEach { withExternal ->

                transitions +=
                    parseExternalTransition(
                        withExternal
                    )
            }

        return transitions
    }

    private fun parseExternalTransition(
        withExternal: PsiMethodCallExpression
    ): List<SsmTransition> {

        /*
         * Собираем ТОЛЬКО текущий transition.
         *
         * Например:
         *
         * withExternal()
         *   .source(...)
         *   .target(...)
         *   .event(...)
         *   .guard(...)
         *   .action(...)
         *
         * Как только встречаем .and(),
         * текущий transition закончился.
         */
        val chain =
            collectFluentChain(
                withExternal
            )

        val sourceCall =
            chain.findCall("source")
                ?: return emptyList()

        val targetCall =
            chain.findCall("target")
                ?: return emptyList()

        val sourceExpression =
            sourceCall
                .argumentList
                .expressions
                .firstOrNull()
                ?: return emptyList()

        val targetExpression =
            targetCall
                .argumentList
                .expressions
                .firstOrNull()
                ?: return emptyList()

        val sources =
            resolve(
                sourceExpression
            )

        val targets =
            resolve(
                targetExpression
            )

        if (
            sources.isEmpty() ||
            targets.isEmpty()
        ) {
            return emptyList()
        }

        /*
         * Event.
         */
        val eventExpression =
            chain
                .findCall("event")
                ?.argumentList
                ?.expressions
                ?.firstOrNull()

        /*
         * Guard.
         *
         * Теперь guard ищется только
         * внутри текущего transition.
         */
        val guardExpression =
            chain
                .findCall("guard")
                ?.argumentList
                ?.expressions
                ?.firstOrNull()

        /*
         * Action + errorAction.
         */
        val actions =
            chain
                .filter {
                    it.methodExpression.referenceName == "action"
                }
                .mapNotNull(::parseAction)

        val event =
            eventExpression
                ?.let(::resolveSingleName)

        val guard =
            guardExpression
                ?.let(componentResolver::resolve)



        /*
         * Cartesian product.
         *
         * Например если source:
         *
         * [CREATED, CHECKING, READY]
         *
         * создаём отдельный transition
         * для каждого source.
         */
        return sources.flatMap { source ->

            targets.map { target ->

                SsmTransition(
                    source = source.name,
                    target = target.name,

                    event = event,
                    guard = guard,
                    actions = actions,

                    sourceElement = source.source,
                    targetElement = target.source,

                    eventElement = eventExpression
                        ?.let {
                            expressionResolver
                                .resolve(it)
                                .firstOrNull()
                                ?.source
                                ?: it
                        }
                )
            }
        }
    }

    private fun parseAction(
        call: PsiMethodCallExpression
    ): SsmAction? {

        val arguments =
            call.argumentList.expressions

        val actionExpression =
            arguments.getOrNull(0)
                ?: return null

        val errorActionExpression =
            arguments.getOrNull(1)

        val action =
            componentResolver.resolve(actionExpression)
                ?: return null

        val errorAction =
            errorActionExpression
                ?.let(componentResolver::resolve)

        return SsmAction(
            action = action,
            errorAction = errorAction
        )
    }

    private fun resolve(
        expression: PsiExpression
    ): List<ResolvedValue> {

        return expressionResolver
            .resolve(expression)
    }

    private fun resolveSingleName(
        expression: PsiExpression
    ): String? {

        return expressionResolver
            .resolve(expression)
            .firstOrNull()
            ?.name
            ?: resolveReferenceName(
                expression
            )
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

}
