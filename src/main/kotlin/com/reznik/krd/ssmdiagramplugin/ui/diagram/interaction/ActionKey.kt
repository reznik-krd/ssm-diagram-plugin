package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction



/**
 * Однозначно идентифицирует action по переходу и индексу внутри него.
 */
data class ActionKey(
    val edgeId: String,
    val actionIndex: Int
)
