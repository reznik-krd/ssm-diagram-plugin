package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ExpressionResolver
import com.reznik.krd.ssmdiagramplugin.analyzer.expression.ResolvedValue
import com.reznik.krd.ssmdiagramplugin.model.SsmState

/**
 * Извлекает состояния StateMachine из метода настройки состояний.
 */
class SsmStateAnalyzer {

    private val expressionResolver = ExpressionResolver()

    /**
     * Анализирует переданный PSI-элемент и возвращает соответствующую модель.
     */
    fun analyze(configClass: PsiClass): List<SsmState> {

        val configureMethod = findStatesConfigureMethod(configClass)
            ?: return emptyList()

        val methodCalls = PsiTreeUtil.findChildrenOfType(configureMethod, PsiMethodCallExpression::class.java)

        val states = linkedMapOf<String, SsmState>()

        methodCalls.forEach { call ->
            when (call.methodExpression.referenceName) {
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

        return states.values.toList()
    }

    /**
     * Находит метод configure, отвечающий за объявление состояний.
     */
    private fun findStatesConfigureMethod(configClass: PsiClass): PsiMethod? {
        return configClass
            .findMethodsByName("configure", false)
            .firstOrNull { method ->
                method.parameterList
                    .parameters
                    .any { parameter ->
                        parameter.type
                            .canonicalText
                            .contains("StateMachineStateConfigurer")
                    }
            }
    }

    /**
     * Разрешает состояния из аргумента DSL-вызова и добавляет их в результат.
     */
    private fun addStates(call: PsiMethodCallExpression, type: SsmState.Type, states: MutableMap<String, SsmState>) {
        val argument = call.argumentList.expressions.firstOrNull() ?: return
        val resolvedStates = expressionResolver.resolve(argument)

        resolvedStates.forEach { resolved ->
            addResolvedState(
                resolved = resolved,
                type = type,
                states = states
            )
        }
    }

    /**
     * Добавляет разрешённое состояние с учётом его специального типа.
     */
    private fun addResolvedState(resolved: ResolvedValue, type: SsmState.Type, states: MutableMap<String, SsmState>) {
        val existing = states[resolved.name]

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
        if (existing == null || shouldReplace(existing.type, type)) {
            states[resolved.name] =
                SsmState(
                    name = resolved.name,
                    type = type,
                    source = resolved.source
                )
        }
    }

    /**
     * Определяет, нужно ли заменить ранее найденный тип состояния более точным.
     */
    private fun shouldReplace(existingType: SsmState.Type, newType: SsmState.Type): Boolean {
        if (existingType == SsmState.Type.NORMAL) {
            return true
        }
        if (newType == SsmState.Type.NORMAL) {
            return false
        }
        return true
    }
}