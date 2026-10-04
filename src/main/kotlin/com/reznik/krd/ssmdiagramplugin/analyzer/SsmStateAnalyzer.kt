package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ExpressionResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ResolvedValue
import com.reznik.krd.ssmdiagramplugin.model.SsmState

/**
 * Extracts declared initial, normal, choice and end states
 * from a StateMachine configuration.
 */
class SsmStateAnalyzer {

    private val expressionResolver =
        ExpressionResolver()

    /**
     * Extracts all states declared by the configuration class.
     */
    fun analyze(
        configClass: PsiClass
    ): List<SsmState> {

        val configureMethod =
            findStatesConfigureMethod(
                configClass
            ) ?: return emptyList()

        val methodCalls =
            PsiTreeUtil.findChildrenOfType(
                configureMethod,
                PsiMethodCallExpression::class.java
            )

        val states =
            linkedMapOf<String, SsmState>()

        methodCalls.forEach { call ->

            when (
                call.methodExpression.referenceName
            ) {

                "initial" ->
                    addStates(
                        call = call,
                        type = SsmState.Type.INITIAL,
                        states = states
                    )

                "state" ->
                    addStates(
                        call = call,
                        type = SsmState.Type.NORMAL,
                        states = states
                    )

                /*
                 * Important:
                 *
                 * .states(Set.of(...))
                 *
                 * may produce many states.
                 */
                "states" ->
                    addStates(
                        call = call,
                        type = SsmState.Type.NORMAL,
                        states = states
                    )

                "choice" ->
                    addStates(
                        call = call,
                        type = SsmState.Type.CHOICE,
                        states = states
                    )

                "end" ->
                    addStates(
                        call = call,
                        type = SsmState.Type.END,
                        states = states
                    )
            }
        }

        return states.values
            .toList()
    }

    // -------------------------------------------------------------------------
    // Configure method
    // -------------------------------------------------------------------------

    private fun findStatesConfigureMethod(
        configClass: PsiClass
    ): PsiMethod? {

        return configClass
            .findMethodsByName(
                "configure",
                false
            )
            .firstOrNull { method ->

                method.parameterList
                    .parameters
                    .any { parameter ->

                        parameter.type
                            .canonicalText
                            .contains(
                                "StateMachineStateConfigurer"
                            )
                    }
            }
    }

    // -------------------------------------------------------------------------
    // State extraction
    // -------------------------------------------------------------------------

    private fun addStates(
        call: PsiMethodCallExpression,
        type: SsmState.Type,
        states: MutableMap<String, SsmState>
    ) {

        val argument =
            call.argumentList
                .expressions
                .firstOrNull()
                ?: return

        val resolvedStates =
            expressionResolver
                .resolve(argument)

        resolvedStates.forEach { resolved ->

            addResolvedState(
                resolved = resolved,
                type = type,
                states = states
            )
        }
    }

    private fun addResolvedState(
        resolved: ResolvedValue,
        type: SsmState.Type,
        states: MutableMap<String, SsmState>
    ) {

        val existing =
            states[resolved.name]

        /*
         * INITIAL / CHOICE / END
         * have higher semantic priority
         * than NORMAL.
         *
         * Example:
         *
         * .states(Set.of(CarState.values()))
         * .choice(CarState.PRE_CLASSIFICATION)
         *
         * PRE_CLASSIFICATION is first discovered
         * as NORMAL and then upgraded to CHOICE.
         */
        if (
            existing == null ||
            shouldReplace(
                existing.type,
                type
            )
        ) {

            states[resolved.name] =
                SsmState(
                    name = resolved.name,
                    type = type,

                    /*
                     * Important:
                     *
                     * source is PsiEnumConstant,
                     * not the expression in config.
                     *
                     * Clicking the state therefore
                     * navigates directly to enum.
                     */
                    source = resolved.source
                )
        }
    }

    private fun shouldReplace(
        existingType: SsmState.Type,
        newType: SsmState.Type
    ): Boolean {

        if (
            existingType ==
            SsmState.Type.NORMAL
        ) {
            return true
        }

        /*
         * Do not downgrade a special state
         * back to NORMAL.
         */
        if (
            newType ==
            SsmState.Type.NORMAL
        ) {
            return false
        }

        /*
         * If two special declarations exist,
         * the later explicit declaration wins.
         */
        return true
    }
}