package com.reznik.krd.ssmdiagramplugin.ui.diagram.model

import com.reznik.krd.ssmdiagramplugin.model.SsmState

data class DiagramNode(
    val id: String,
    val logicalId: String,
    val label: String,
    val type: Type,
    val state: SsmState? = null,
    val visualCopy: Boolean = false
) {

    enum class Type {
        INITIAL_MARKER,
        STATE,
        END_MARKER,
        CHOICE
    }
}