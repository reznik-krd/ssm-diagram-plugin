package com.reznik.krd.ssmdiagramplugin.ui.diagram.transform

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode

/**
 * Introduces visual state copies when doing so reduces high fan-in diagram clutter.
 */
class ReadableGraphTransformer {

    companion object {

        /*
         * Пока консервативно.
         *
         * Два входа часто являются обычным merge.
         * Три и более уже часто создают визуальную кашу.
         */
        private const val MIN_DISTINCT_SOURCES_FOR_DUPLICATION = 3
    }

    /** Produces a display-oriented graph while preserving logical state identity. */
    fun transform(
        graph: DiagramGraph
    ): DiagramGraph {

        val nodes =
            graph.nodes.toMutableList()

        val edges =
            graph.edges.toMutableList()

        val candidates =
            findDuplicationCandidates(graph)

        candidates.forEach { candidate ->

            duplicateIncomingEdges(
                candidate,
                nodes,
                edges
            )
        }

        return DiagramGraph(
            nodes = nodes,
            edges = edges
        )
    }

    private fun findDuplicationCandidates(
        graph: DiagramGraph
    ): List<DiagramNode> {

        return graph.nodes.filter { node ->

            if (node.type != DiagramNode.Type.STATE) {
                return@filter false
            }

            val incoming =
                graph.incoming(node.id)
                    .filter {
                        it.type == DiagramEdge.Type.TRANSITION
                    }

            val distinctSources =
                incoming
                    .map { it.source }
                    .distinct()

            distinctSources.size >=
                    MIN_DISTINCT_SOURCES_FOR_DUPLICATION
        }
    }

    private fun duplicateIncomingEdges(
        node: DiagramNode,
        nodes: MutableList<DiagramNode>,
        edges: MutableList<DiagramEdge>
    ) {

        val incoming =
            edges
                .filter {
                    it.target == node.id &&
                            it.type == DiagramEdge.Type.TRANSITION
                }

        val outgoing =
            edges.filter {
                it.source == node.id &&
                        (
                                it.type ==
                                        DiagramEdge.Type.TRANSITION ||
                                        it.type ==
                                        DiagramEdge.Type.CHOICE_BRANCH
                                )
            }

        if (incoming.isEmpty()) {
            return
        }

        /*
         * Если state имеет дальнейший flow,
         * оставляем один incoming на настоящий node.
         *
         * Остальные входы превращаем в visual copies.
         *
         * Если outgoing нет — можно размножить вообще
         * все incoming.
         */
        val edgesToDuplicate =
            if (outgoing.isEmpty()) {
                incoming
            } else {
                incoming.drop(1)
            }

        /*
         * Terminal state:
         * оригинальный visual node больше не нужен,
         * потому что каждый incoming получит свою копию.
         */
        if (outgoing.isEmpty()) {

            nodes.removeIf {
                it.id == node.id
            }

            edges.removeAll(incoming.toSet())
        } else {

            /*
             * Первый incoming остаётся связан
             * с настоящим node.
             *
             * Остальные сейчас перенаправим.
             */
            edges.removeAll(
                edgesToDuplicate.toSet()
            )
        }

        edgesToDuplicate.forEachIndexed { index, edge ->

            val copyId =
                createCopyId(
                    node,
                    edge,
                    index
                )

            nodes += node.copy(
                id = copyId,
                visualCopy = true
            )

            edges += edge.copy(
                id = "${edge.id}__copy__$index",
                target = copyId
            )
        }
    }

    private fun createCopyId(
        node: DiagramNode,
        edge: DiagramEdge,
        index: Int
    ): String {

        return buildString {

            append(node.logicalId)

            append("__copy__")

            append(edge.source)

            append("__")

            append(edge.id)

            append("__")

            append(index)
        }
    }
}