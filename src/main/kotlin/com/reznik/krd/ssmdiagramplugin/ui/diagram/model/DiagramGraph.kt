package com.reznik.krd.ssmdiagramplugin.ui.diagram.model

/**
 * Хранит визуальные узлы и рёбра диаграммы и предоставляет быстрые выборки связей.
 */
data class DiagramGraph(
    val nodes: List<DiagramNode>,
    val edges: List<DiagramEdge>
) {

    /** Находит визуальный узел графа по идентификатору. */
    fun node(id: String): DiagramNode? {
        return nodes.firstOrNull { it.id == id }
    }

    /** Возвращает исходящие из узла переходы. */
    fun outgoing(nodeId: String): List<DiagramEdge> {
        return edges.filter { it.source == nodeId }
    }

    /** Возвращает входящие в узел переходы. */
    fun incoming(nodeId: String): List<DiagramEdge> {
        return edges.filter { it.target == nodeId }
    }
}