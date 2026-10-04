package com.reznik.krd.ssmdiagramplugin.model

/**
 * Описывает action перехода и связанную с ним ссылку на компонент.
 */
data class SsmAction(
    val action: SsmComponentReference,
    val errorAction: SsmComponentReference? = null
)