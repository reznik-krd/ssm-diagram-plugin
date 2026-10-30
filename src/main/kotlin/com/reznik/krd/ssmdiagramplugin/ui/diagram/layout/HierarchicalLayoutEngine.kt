package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import java.awt.Dimension
import java.util.ArrayDeque

/**
 * Располагает узлы графа по уровням и рассчитывает геометрию диаграммы.
 */
class HierarchicalLayoutEngine {

    private val edgeRouter = OrthogonalEdgeRouter()


    /**
     * Рассчитывает положение узлов, маршруты рёбер и размер диаграммы.
     */
    fun layout(graph: DiagramGraph): DiagramLayout {
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
        val levels = calculateLevels(graph)

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
        orderLayers(graph = graph, layers = layers)

        /*
         * 4. Назначаем реальные координаты.
         */
        val nodes = assignCoordinates(layers)

        /*
         * 5. Только после окончательного положения
         * nodes строим edges.
         */
        val edges = edgeRouter.route(graph = graph, nodes = nodes)

        /*
         * 6. Размер canvas считаем по фактическим nodes.
         */
        val size = calculateCanvasSize(nodes)

        return DiagramLayout(
            nodes = nodes,
            edges = edges,
            size = size
        )
    }

    /**
     * Распределяет узлы по вертикальным уровням графа.
     */
    private fun calculateLevels(graph: DiagramGraph): Map<String, Int> {
        val levels = mutableMapOf<String, Int>()
        val roots = findRoots(graph)
        val queue = ArrayDeque<DiagramNode>()

        roots.forEach { root ->
            levels[root.id] = 0
            queue.add(root)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentLevel = levels[current.id] ?: 0
            graph.outgoing(current.id)
                .forEach { edge ->
                    val target =
                        graph.node(edge.target)
                            ?: return@forEach

                    if (target.id in levels) {
                        return@forEach
                    }

                    levels[target.id] = currentLevel + 1

                    queue.add(target)
                }
        }

        var fallbackLevel = (levels.values.maxOrNull() ?: -1) + 1

        graph.nodes
            .filter { it.id !in levels }
            .forEach { node ->
                levels[node.id] =
                    fallbackLevel
            }

        return levels
    }

    /**
     * Упорядочивает узлы внутри уровней для уменьшения пересечений.
     */
    private fun orderLayers(
        graph: DiagramGraph,
        layers: MutableMap<Int, MutableList<DiagramNode>>
    ) {
        val layerNumbers = layers.keys.sorted()

        layerNumbers
            .drop(1)
            .forEach { level ->
                val currentLayer = layers[level]
                    ?: return@forEach

                val previousLayer = layers[level - 1]
                    ?: return@forEach

                val previousPositions = previousLayer
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
     * Вычисляет среднюю позицию родителей для сортировки узлов слоя.
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
     * Назначает итоговые координаты узлам.
     */
    private fun assignCoordinates(
        layers: Map<Int, List<DiagramNode>>
    ): MutableMap<String, NodeLayout> {

        val result = mutableMapOf<String, NodeLayout>()
        val maxNodesInLayer = layers.values.maxOfOrNull { it.size } ?: 1

        val maxLayerWidth = calculateLayerWidth(maxNodesInLayer)

        layers.forEach { (level, layerNodes) ->
            val layerWidth = calculateLayerWidth(layerNodes.size)

            /*
             * Каждый ряд центрируем относительно
             * самого широкого ряда.
             */
            var x = LayoutDimensions.CANVAS_MARGIN + (maxLayerWidth - layerWidth) / 2
            val y = LayoutDimensions.CANVAS_MARGIN + level * (LayoutDimensions.NODE_HEIGHT + LayoutDimensions.VERTICAL_GAP)

            layerNodes.forEach { node ->
                result[node.id] = NodeLayout(x = x, y = y, width = LayoutDimensions.NODE_WIDTH, height = LayoutDimensions.NODE_HEIGHT)
                x += LayoutDimensions.NODE_WIDTH + LayoutDimensions.HORIZONTAL_GAP
            }
        }

        return result
    }

    /**
     * Вычисляет ширину одного уровня графа.
     */
    private fun calculateLayerWidth(nodeCount: Int): Int {
        if (nodeCount <= 0) {
            return 0
        }
        return nodeCount * LayoutDimensions.NODE_WIDTH + (nodeCount - 1) * LayoutDimensions.HORIZONTAL_GAP
    }

    /**
     * Вычисляет необходимый размер области диаграммы.
     */
    private fun calculateCanvasSize(nodes: Map<String, NodeLayout>): Dimension {
        if (nodes.isEmpty()) {
            return Dimension(600, 500)
        }

        val maxX = nodes.values.maxOf { it.x + it.width }
        val maxY = nodes.values.maxOf { it.y + it.height }

        return Dimension(
            maxOf(maxX + LayoutDimensions.CANVAS_MARGIN, 600),
            maxOf(maxY + LayoutDimensions.CANVAS_MARGIN, 500)
        )
    }

    /**
     * Находит корневые узлы для начала иерархического обхода.
     */
    private fun findRoots(graph: DiagramGraph): List<DiagramNode> {

        /*
         * Наш виртуальный Spring INITIAL
         * всегда главный root.
         */
        val initialNodes = graph.nodes.filter { it.type == DiagramNode.Type.INITIAL_MARKER }

        if (initialNodes.isNotEmpty()) {
            return initialNodes
        }

        /*
         * Fallback для графов без explicit initial.
         */
        val roots = graph.nodes.filter {
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