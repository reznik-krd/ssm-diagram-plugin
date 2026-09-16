package com.reznik.krd.ssmdiagramplugin.model

/**
 * Normal transition action and its optional error action.
 */
data class SsmAction(
    val action: SsmComponentReference,
    val errorAction: SsmComponentReference? = null
)