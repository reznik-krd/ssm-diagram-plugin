package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import java.awt.Dimension
import java.util.ArrayDeque

class HierarchicalLayoutEngine {

    private val edgeRouter =
        OrthogonalEdgeRouter()

    private val nodeWidth = 180
    private val nodeHeight = 60

    /*
     * Нам теперь важнее читаемость, чем компактность.
     *
     * Большой horizontalGap даёт место:
     * - для labels;
     * - для параллельных edges;
     * - для guards.
     */
    private val horizontalGap = 140

    /*
 * Между уровнями оставляем место для:
 *
 * - actions предыдущего transition;
 * - event;
 * - guard;
 * - routing.
 */
    private val verticalGap = 180

    private val margin = 100

    fun layout(
        graph: DiagramGraph
    ): DiagramLayout {

        if (graph.nodes.isEmpty()) {
            return DiagramLayout(
                nodes = emptyMap(),
                edges = emptyMap(),
                size = Dimension(600, 500)
            )
        }

        /*
         * 1. Определяем уровень каждого node.
         */
        val levels =
            calculateLevels(graph)

        /*
         * 2. Группируем nodes по уровням.
         */
        val layers =
            graph.nodes
                .groupBy {
                    levels[it.id] ?: 0
                }
                .toSortedMap()
                .mapValues { (_, nodes) ->
                    nodes.toMutableList()
                }
                .toMutableMap()

        /*
         * 3. Сортируем nodes внутри уровней.
         *
         * Это уже первая простая попытка
         * уменьшить пересечения.
         */
        orderLayers(
            graph = graph,
            layers = layers
        )

        /*
         * 4. Назначаем реальные координаты.
         */
        val nodes =
            assignCoordinates(
                layers
            )

        /*
         * 5. Только после окончательного положения
         * nodes строим edges.
         */
        val edges =
            edgeRouter.route(
                graph = graph,
                nodes = nodes
            )

        /*
         * 6. Размер canvas считаем по фактическим nodes.
         */
        val size =
            calculateCanvasSize(
                nodes
            )

        return DiagramLayout(
            nodes = nodes,
            edges = edges,
            size = size
        )
    }

    /**
     * Определяет вертикальный layer каждого node.
     *
     * Основная идея:
     *
     * parent
     *   |
     *   v
     * child
     *
     * child всегда получает следующий уровень.
     *
     * Back edges не заставляют бесконечно
     * двигать вершины вниз.
     */
    private fun calculateLevels(
        graph: DiagramGraph
    ): Map<String, Int> {

        val levels =
            mutableMapOf<String, Int>()

        val roots =
            findRoots(graph)

        val queue =
            ArrayDeque<DiagramNode>()

        roots.forEach { root ->

            levels[root.id] = 0

            queue.add(root)
        }

        while (queue.isNotEmpty()) {

            val current =
                queue.removeFirst()

            val currentLevel =
                levels[current.id] ?: 0

            graph.outgoing(current.id)
                .forEach { edge ->

                    val target =
                        graph.node(edge.target)
                            ?: return@forEach

                    /*
                     * Уже посещённую вершину повторно
                     * вниз не двигаем.
                     *
                     * Это защищает от циклов/back edges.
                     */
                    if (target.id in levels) {
                        return@forEach
                    }

                    levels[target.id] =
                        currentLevel + 1

                    queue.add(target)
                }
        }

        /*
         * Если существуют disconnected nodes,
         * кладём их после основной StateMachine.
         */
        var fallbackLevel =
            (levels.values.maxOrNull() ?: -1) + 1

        graph.nodes
            .filter {
                it.id !in levels
            }
            .forEach { node ->

                levels[node.id] =
                    fallbackLevel
            }

        return levels
    }

    /**
     * Упорядочиваем каждый layer относительно
     * родителей предыдущего layer.
     *
     * Пока это простая версия barycenter.
     */
    private fun orderLayers(
        graph: DiagramGraph,
        layers: MutableMap<Int, MutableList<DiagramNode>>
    ) {

        val layerNumbers =
            layers.keys.sorted()

        /*
         * Самый верхний layer сортировать
         * относительно родителей бессмысленно.
         */
        layerNumbers
            .drop(1)
            .forEach { level ->

                val currentLayer =
                    layers[level]
                        ?: return@forEach

                val previousLayer =
                    layers[level - 1]
                        ?: return@forEach

                val previousPositions =
                    previousLayer
                        .mapIndexed { index, node ->
                            node.id to index
                        }
                        .toMap()

                currentLayer.sortBy { node ->

                    calculateParentBarycenter(
                        graph = graph,
                        node = node,
                        previousPositions =
                            previousPositions
                    )
                }
            }
    }

    /**
     * Средняя позиция родителей.
     *
     * Например:
     *
     * A   B   C
     *     \ /
     *      X
     *
     * X будет стремиться находиться
     * между B и C.
     */
    private fun calculateParentBarycenter(
        graph: DiagramGraph,
        node: DiagramNode,
        previousPositions: Map<String, Int>
    ): Double {

        val parentPositions =
            graph.incoming(node.id)
                .mapNotNull { edge ->
                    previousPositions[edge.source]
                }

        if (parentPositions.isEmpty()) {
            return Double.MAX_VALUE
        }

        return parentPositions.average()
    }

    /**
     * После того как известны layers и порядок nodes
     * внутри них, назначаем координаты.
     */
    private fun assignCoordinates(
        layers: Map<Int, List<DiagramNode>>
    ): MutableMap<String, NodeLayout> {

        val result =
            mutableMapOf<String, NodeLayout>()

        val maxNodesInLayer =
            layers.values
                .maxOfOrNull {
                    it.size
                }
                ?: 1

        val maxLayerWidth =
            calculateLayerWidth(
                maxNodesInLayer
            )

        layers.forEach { (level, layerNodes) ->

            val layerWidth =
                calculateLayerWidth(
                    layerNodes.size
                )

            /*
             * Каждый ряд центрируем относительно
             * самого широкого ряда.
             */
            var x =
                margin +
                        (maxLayerWidth - layerWidth) / 2

            val y =
                margin +
                        level *
                        (nodeHeight + verticalGap)

            layerNodes.forEach { node ->

                result[node.id] =
                    NodeLayout(
                        x = x,
                        y = y,
                        width = nodeWidth,
                        height = nodeHeight
                    )

                x +=
                    nodeWidth +
                            horizontalGap
            }
        }

        return result
    }

    private fun calculateLayerWidth(
        nodeCount: Int
    ): Int {

        if (nodeCount <= 0) {
            return 0
        }

        return nodeCount * nodeWidth +
                (nodeCount - 1) *
                horizontalGap
    }

    private fun calculateCanvasSize(
        nodes: Map<String, NodeLayout>
    ): Dimension {

        if (nodes.isEmpty()) {
            return Dimension(
                600,
                500
            )
        }

        val maxX =
            nodes.values.maxOf {
                it.x + it.width
            }

        val maxY =
            nodes.values.maxOf {
                it.y + it.height
            }

        return Dimension(
            maxOf(
                maxX + margin,
                600
            ),

            maxOf(
                maxY + margin,
                500
            )
        )
    }

    private fun findRoots(
        graph: DiagramGraph
    ): List<DiagramNode> {

        /*
         * Наш виртуальный Spring INITIAL
         * всегда главный root.
         */
        val initialNodes =
            graph.nodes.filter {
                it.type ==
                        DiagramNode.Type.INITIAL_MARKER
            }

        if (initialNodes.isNotEmpty()) {
            return initialNodes
        }

        /*
         * Fallback для графов без explicit initial.
         */
        val roots =
            graph.nodes.filter {
                graph.incoming(it.id)
                    .isEmpty()
            }

        if (roots.isNotEmpty()) {
            return roots
        }

        /*
         * Например полностью циклический граф.
         */
        return listOf(
            graph.nodes.first()
        )
    }
}