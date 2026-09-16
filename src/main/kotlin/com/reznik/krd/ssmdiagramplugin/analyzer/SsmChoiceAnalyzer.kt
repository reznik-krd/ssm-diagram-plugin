package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ComponentReferenceResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ExpressionResolver
import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.model.SsmChoice
import com.reznik.krd.ssmdiagramplugin.model.SsmChoiceBranch

class SsmChoiceAnalyzer {

    private val expressionResolver = ExpressionResolver()
    private val componentResolver = ComponentReferenceResolver()

    fun analyze(configClass: PsiClass): List<SsmChoice> {
        val result = mutableListOf<SsmChoice>()

        val calls = PsiTreeUtil.findChildrenOfType(
            configClass,
            PsiMethodCallExpression::class.java
        )

        calls
            .filter {
                it.methodExpression.referenceName == "withChoice"
            }
            .forEach { withChoice ->
                parseChoice(withChoice)?.let(result::add)
            }

        return result
    }

    private fun parseChoice(
        withChoice: PsiMethodCallExpression
    ): SsmChoice? {

        val chain = collectChain(withChoice)

        val sourceCall =
            chain.findCall("source") ?: return null

        val sourceExpression =
            sourceCall.argumentList.expressions.firstOrNull()
                ?: return null

        val source = expressionResolver
            .resolve(sourceExpression)
            .firstOrNull()
            ?: return null

        val branches = mutableListOf<SsmChoiceBranch>()

        chain.forEach { call ->

            when (call.methodExpression.referenceName) {

                "first" -> {
                    parseGuardedBranch(
                        call,
                        SsmChoiceBranch.Type.FIRST
                    )?.let(branches::add)
                }

                "then" -> {
                    parseGuardedBranch(
                        call,
                        SsmChoiceBranch.Type.THEN
                    )?.let(branches::add)
                }

                "last" -> {
                    parseLastBranch(call)
                        ?.let(branches::add)
                }
            }
        }

        if (branches.isEmpty()) {
            return null
        }

        return SsmChoice(
            source = source.name,
            branches = branches,
            sourceElement = source.source
        )
    }

    private fun parseGuardedBranch(
        call: PsiMethodCallExpression,
        type: SsmChoiceBranch.Type
    ): SsmChoiceBranch? {

        val arguments =
            call.argumentList.expressions

        val targetExpression =
            arguments.getOrNull(0)
                ?: return null

        val guardExpression =
            arguments.getOrNull(1)

        val actionExpression =
            arguments.getOrNull(2)

        val errorActionExpression =
            arguments.getOrNull(3)

        val target =
            expressionResolver
                .resolve(targetExpression)
                .firstOrNull()
                ?: return null

        val action =
            actionExpression
                ?.let(componentResolver::resolve)

        val errorAction =
            errorActionExpression
                ?.let(componentResolver::resolve)

        val actions =
            if (action != null) {
                listOf(
                    SsmAction(
                        action = action,
                        errorAction = errorAction
                    )
                )
            } else {
                emptyList()
            }

        return SsmChoiceBranch(
            target = target.name,
            type = type,

            guard = guardExpression
                ?.let(componentResolver::resolve),

            actions = actions,

            targetElement = target.source
        )
    }

    private fun parseLastBranch(
        call: PsiMethodCallExpression
    ): SsmChoiceBranch? {

        val arguments =
            call.argumentList.expressions

        val targetExpression =
            arguments.getOrNull(0)
                ?: return null

        val actionExpression =
            arguments.getOrNull(1)

        val errorActionExpression =
            arguments.getOrNull(2)

        val target =
            expressionResolver
                .resolve(targetExpression)
                .firstOrNull()
                ?: return null

        val action =
            actionExpression
                ?.let(componentResolver::resolve)

        val errorAction =
            errorActionExpression
                ?.let(componentResolver::resolve)

        val actions =
            if (action != null) {
                listOf(
                    SsmAction(
                        action = action,
                        errorAction = errorAction
                    )
                )
            } else {
                emptyList()
            }

        return SsmChoiceBranch(
            target = target.name,
            type = SsmChoiceBranch.Type.LAST,
            guard = null,
            actions = actions,
            targetElement = target.source
        )
    }

    private fun collectChain(
        start: PsiMethodCallExpression
    ): List<PsiMethodCallExpression> {

        val result =
            mutableListOf<PsiMethodCallExpression>()

        var current:
                PsiMethodCallExpression? =
            start

        while (current != null) {

            val methodName =
                current
                    .methodExpression
                    .referenceName

            /*
             * .and() завершает текущий fluent-блок.
             *
             * Всё после него уже относится
             * к следующей конфигурации.
             */
            if (methodName == "and") {
                break
            }

            result += current

            val parentReference =
                current.parent
                        as? PsiReferenceExpression
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