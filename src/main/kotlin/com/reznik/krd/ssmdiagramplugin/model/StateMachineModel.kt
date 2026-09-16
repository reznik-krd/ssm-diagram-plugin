package com.reznik.krd.ssmdiagramplugin.model

/**
 * Complete semantic model extracted from one Spring StateMachine configuration.
 */
data class StateMachineModel(
    val config: SsmConfigInfo,
    val states: List<SsmState>,
    val transitions: List<SsmTransition>,
    val choices: List<SsmChoice>
)