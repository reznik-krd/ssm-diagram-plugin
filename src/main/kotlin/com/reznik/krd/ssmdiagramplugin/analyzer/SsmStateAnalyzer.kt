package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.model.SsmState

/**
 * Extracts declared initial, normal, choice and end states from a StateMachine configuration.
 */
class SsmStateAnalyzer {

    /** Extracts all states declared by the configuration class. */
    fun analyze(configClass: PsiClass): List<SsmState> {
        val configureMethod = findStatesConfigureMethod(configClass)
            ?: return emptyList()

        val methodCalls = PsiTreeUtil.findChildrenOfType(
            configureMethod,
            PsiMethodCallExpression::class.java
        )

        val states = linkedMapOf<String, SsmState>()

        methodCalls.forEach { call ->
            when (call.methodExpression.referenceName) {
                "initial" -> addState(call, SsmState.Type.INITIAL, states)
                "state" -> addState(call, SsmState.Type.NORMAL, states)
                "choice" -> addState(call, SsmState.Type.CHOICE, states)
                "end" -> addState(call, SsmState.Type.END, states)
            }
        }

        return states.values.toList()
    }

    private fun findStatesConfigureMethod(configClass: PsiClass): PsiMethod? {
        return configClass.findMethodsByName("configure", false)
            .firstOrNull { method ->
                method.parameterList.parameters.any { parameter ->
                    parameter.type.canonicalText.contains(
                        "StateMachineStateConfigurer"
                    )
                }
            }
    }

    private fun addState(
        call: PsiMethodCallExpression,
        type: SsmState.Type,
        states: MutableMap<String, SsmState>
    ) {
        val argument = call.argumentList.expressions.firstOrNull()
            ?: return

        val stateName = resolveStateName(argument)
            ?: return

        val existing = states[stateName]

        // INITIAL / CHOICE / END важнее обычного state()
        if (existing == null || existing.type == SsmState.Type.NORMAL) {
            states[stateName] = SsmState(
                name = stateName,
                type = type,
                source = argument
            )
        }
    }

    private fun resolveStateName(expression: PsiExpression): String? {
        if (expression is PsiReferenceExpression) {
            return expression.referenceName
        }

        return null
    }
}