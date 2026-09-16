package com.reznik.krd.ssmdiagramplugin.ui.diagram.builder

import com.reznik.krd.ssmdiagramplugin.model.SsmState
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode

/**
 * Converts the semantic StateMachineModel into the canonical visual graph.
 */
class DiagramGraphBuilder {

    companion object {
        private const val INITIAL_NODE_ID = "__INITIAL__"
        private const val END_NODE_PREFIX = "__END__"
    }

    /** Builds the canonical visual graph from the semantic model. */
    fun build(model: StateMachineModel): DiagramGraph {

        val nodes = mutableListOf<DiagramNode>()
        val edges = mutableListOf<DiagramEdge>()

        var edgeIndex = 0

        model.states.forEach { state ->

            nodes += DiagramNode(
                id = state.name,
                logicalId = state.name,
                label = state.name,

                type = when (state.type) {

                    SsmState.Type.CHOICE ->
                        DiagramNode.Type.CHOICE

                    else ->
                        DiagramNode.Type.STATE
                },

                state = state
            )
        }

        model.transitions.forEach { transition ->

            edges += DiagramEdge(
                id = "transition_${edgeIndex++}",

                source = transition.source,
                target = transition.target,

                type = DiagramEdge.Type.TRANSITION,

                event = transition.event,
                eventElement = transition.eventElement,
                guard = transition.guard,
                actions = transition.actions
            )
        }

        model.choices.forEach { choice ->

            choice.branches.forEach { branch ->

                edges += DiagramEdge(
                    id = "choice_${edgeIndex++}",

                    source = choice.source,
                    target = branch.target,

                    type = DiagramEdge.Type.CHOICE_BRANCH,

                    guard = branch.guard,
                    actions = branch.actions
                )
            }
        }

        addInitialMarker(
            model,
            nodes,
            edges,
            edgeIndex++
        )

        addEndMarkers(
            model,
            nodes,
            edges,
            edgeIndex
        )

        return DiagramGraph(
            nodes = nodes,
            edges = edges
        )
    }

    private fun addInitialMarker(
        model: StateMachineModel,
        nodes: MutableList<DiagramNode>,
        edges: MutableList<DiagramEdge>,
        edgeIndex: Int
    ) {

        val initialState =
            model.states.firstOrNull {
                it.type == SsmState.Type.INITIAL
            } ?: return

        nodes += DiagramNode(
            id = INITIAL_NODE_ID,
            logicalId = INITIAL_NODE_ID,
            label = "",
            type = DiagramNode.Type.INITIAL_MARKER
        )

        edges += DiagramEdge(
            id = "initial_$edgeIndex",
            source = INITIAL_NODE_ID,
            target = initialState.name,
            type = DiagramEdge.Type.INITIAL
        )
    }

    private fun addEndMarkers(
        model: StateMachineModel,
        nodes: MutableList<DiagramNode>,
        edges: MutableList<DiagramEdge>,
        startEdgeIndex: Int
    ) {

        var edgeIndex = startEdgeIndex

        model.states
            .filter {
                it.type == SsmState.Type.END
            }
            .forEach { endState ->

                val markerId =
                    "$END_NODE_PREFIX${endState.name}"

                nodes += DiagramNode(
                    id = markerId,
                    logicalId = markerId,
                    label = "",
                    type = DiagramNode.Type.END_MARKER
                )

                edges += DiagramEdge(
                    id = "end_${edgeIndex++}",
                    source = endState.name,
                    target = markerId,
                    type = DiagramEdge.Type.END
                )
            }
    }
}