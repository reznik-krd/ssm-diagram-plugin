package com.reznik.krd.ssmdiagramplugin.model

data class SsmAction(
    val action: SsmComponentReference,
    val errorAction: SsmComponentReference? = null
)