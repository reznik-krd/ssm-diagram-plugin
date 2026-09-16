package com.reznik.krd.ssmdiagramplugin.model

import com.intellij.psi.PsiClass

data class SsmConfigInfo(
    val configClass: PsiClass,
    val stateClass: PsiClass?,
    val eventClass: PsiClass?
) {
    val configName: String
        get() = configClass.name ?: "Unknown"

    val stateName: String
        get() = stateClass?.name ?: "Unknown"

    val eventName: String
        get() = eventClass?.name ?: "Unknown"
}