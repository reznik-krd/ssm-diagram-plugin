package com.reznik.krd.ssmdiagramplugin.model

/**
 * Агрегирует результат PSI-анализа конфигурации StateMachine.
 */
data class StateMachineModel(
    val config: SsmConfigInfo,
    val states: List<SsmState>,
    val transitions: List<SsmTransition>,
    val choices: List<SsmChoice>
)