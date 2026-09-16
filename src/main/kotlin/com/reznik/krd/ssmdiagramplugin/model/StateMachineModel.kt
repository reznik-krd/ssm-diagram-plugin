package com.reznik.krd.ssmdiagramplugin.model

data class StateMachineModel(
    val config: SsmConfigInfo,
    val states: List<SsmState>,
    val transitions: List<SsmTransition>,
    val choices: List<SsmChoice>
)