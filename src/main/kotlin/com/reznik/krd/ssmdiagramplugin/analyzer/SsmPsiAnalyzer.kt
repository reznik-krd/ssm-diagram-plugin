package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiJavaFile
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel

/**
 * Координирует PSI-анализ конфигурации и собирает итоговую модель StateMachine.
 */
class SsmPsiAnalyzer {

    private val configDetector = SsmConfigDetector()
    private val stateAnalyzer = SsmStateAnalyzer()
    private val transitionAnalyzer = SsmTransitionAnalyzer()
    private val choiceAnalyzer = SsmChoiceAnalyzer()

    /**
     * Анализирует переданный PSI-элемент и возвращает соответствующую модель.
     */
    fun analyze(file: PsiJavaFile): StateMachineModel? {
        val config = configDetector.detect(file) ?: return null
        val states = stateAnalyzer.analyze(config.configClass)
        val transitions = transitionAnalyzer.analyze(config.configClass)
        val choices = choiceAnalyzer.analyze(config.configClass)
        return StateMachineModel(
            config = config,
            states = states,
            transitions = transitions,
            choices = choices
        )
    }
}