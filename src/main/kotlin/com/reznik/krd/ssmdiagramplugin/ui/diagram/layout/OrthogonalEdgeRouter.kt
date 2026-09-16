package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import java.awt.Point

/**
 * Routes diagram edges as readable orthogonal polylines between laid-out nodes.
 */
class OrthogonalEdgeRouter {

    companion object {

        /*
         * Расстояние между параллельными transitions.
         *
         * Например:
         *
         * NEW -> CREATED / START_MANUALLY
         * NEW -> CREATED / START_FROM_GROUP
         */
        private const val PARALLEL_EDGE_GAP = 18

        /*
         * Guard располагаем непосредственно
         * над target state.
         */
        private const val GUARD_TARGET_OFFSET_Y = 12

        /*
         * Event располагаем выше guard.
         *
         * В итоге получаем:
         *
         * EVENT
         *   |
         * GUARD
         *   v
         * STATE
         */
        private const val EVENT_TARGET_OFFSET_Y = 42

        /*
         * Для back edges.
         */
        private const val BACK_EDGE_GAP = 70

        /*
 * Первый action располагается
 * под target state.
 */
        private const val ACTION_TARGET_OFFSET_Y = 24

        /*
         * Расстояние между несколькими actions.
         */
        private const val ACTION_LABEL_GAP = 22
    }

    private fun createActionPositions(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): List<Point> {

        if (edge.actions.isEmpty()) {
            return emptyList()
        }

        return edge.actions.mapIndexed { index, _ ->

            Point(
                target.center.x + parallelOffset,
                target.bottom.y +
                        ACTION_TARGET_OFFSET_Y +
                        index * ACTION_LABEL_GAP
            )
        }
    }

    /** Calculates orthogonal geometry for all graph edges. */
    fun route(
        graph: DiagramGraph,
        nodes: Map<String, NodeLayout>
    ): Map<String, EdgeLayout> {

        val result =
            mutableMapOf<String, EdgeLayout>()

        /*
         * Группируем переходы с одинаковыми
         * source и target.
         *
         * Например:
         *
         * NEW -> CREATED / START_MANUALLY
         * NEW -> CREATED / START_FROM_GROUP
         */
        val groups =
            graph.edges.groupBy { edge ->
                EdgePair(
                    source = edge.source,
                    target = edge.target
                )
            }

        groups.values.forEach { parallelEdges ->

            parallelEdges.forEachIndexed { index, edge ->

                val source =
                    nodes[edge.source]
                        ?: return@forEachIndexed

                val target =
                    nodes[edge.target]
                        ?: return@forEachIndexed

                val parallelOffset =
                    calculateParallelOffset(
                        index = index,
                        count = parallelEdges.size
                    )

                result[edge.id] =
                    routeEdge(
                        edge = edge,
                        source = source,
                        target = target,
                        parallelOffset = parallelOffset
                    )
            }
        }

        return result
    }

    private fun calculateParallelOffset(
        index: Int,
        count: Int
    ): Int {

        if (count <= 1) {
            return 0
        }

        /*
         * Для двух:
         *
         * -9
         * +9
         *
         * Для трёх:
         *
         * -18
         *   0
         * +18
         */
        val center =
            (count - 1) / 2.0

        return (
                (index - center) *
                        PARALLEL_EDGE_GAP
                ).toInt()
    }

    private fun routeEdge(
        edge: DiagramEdge,
        source: NodeLayout,
        target: NodeLayout,
        parallelOffset: Int
    ): EdgeLayout {

        /*
         * Основной случай нашего layered layout:
         *
         * SOURCE
         *   |
         *   v
         * TARGET
         */
        if (target.y > source.y) {
            return routeDownward(
                edge = edge,
                source = source,
                target = target,
                parallelOffset = parallelOffset
            )
        }

        /*
         * Редкий случай:
         *
         * SOURCE ----> TARGET
         */
        if (target.y == source.y) {
            return routeHorizontal(
                edge = edge,
                source = source,
                target = target,
                parallelOffset = parallelOffset
            )
        }

        /*
         * Цикл / переход назад.
         */
        return routeBackward(
            edge = edge,
            source = source,
            target = target,
            parallelOffset = parallelOffset
        )
    }

    private fun routeDownward(
        edge: DiagramEdge,
        source: NodeLayout,
        target: NodeLayout,
        parallelOffset: Int
    ): EdgeLayout {

        val start =
            Point(
                source.bottom.x + parallelOffset,
                source.bottom.y
            )

        val end =
            Point(
                target.top.x + parallelOffset,
                target.top.y
            )

        /*
         * SOURCE и TARGET находятся
         * строго друг под другом.
         */
        if (start.x == end.x) {

            return EdgeLayout(
                points = listOf(
                    start,
                    end
                ),

                /*
                 * Event находится выше guard
                 * и привязан к target.
                 */
                eventLabelPosition =
                    createEventPosition(
                        edge = edge,
                        target = target,
                        parallelOffset = parallelOffset
                    ),

                /*
                 * Guard находится непосредственно
                 * над target.
                 */
                guardLabelPosition =
                    createGuardPosition(
                        edge = edge,
                        target = target,
                        parallelOffset = parallelOffset
                    ),

                actionLabelPositions =
                    createActionPositions(
                        edge = edge,
                        target = target,
                        parallelOffset = parallelOffset
                    )
            )
        }

        /*
         * SOURCE
         *    |
         *    |
         *    +------------------+
         *                       |
         *                       |
         *                       v
         *                     TARGET
         */

        val middleY =
            (start.y + end.y) / 2 +
                    parallelOffset

        val firstCorner =
            Point(
                start.x,
                middleY
            )

        val secondCorner =
            Point(
                end.x,
                middleY
            )

        return EdgeLayout(
            points = listOf(
                start,
                firstCorner,
                secondCorner,
                end
            ),

            /*
             * ВАЖНО:
             *
             * Event больше не находится
             * посередине горизонтальной линии.
             *
             * Он привязан к вертикальному сегменту,
             * который входит в target.
             */
            eventLabelPosition =
                createEventPosition(
                    edge = edge,
                    target = target,
                    parallelOffset = parallelOffset
                ),

            guardLabelPosition =
                createGuardPosition(
                    edge = edge,
                    target = target,
                    parallelOffset = parallelOffset
                ),

            actionLabelPositions =
                createActionPositions(
                    edge = edge,
                    target = target,
                    parallelOffset = parallelOffset
                )
        )
    }

    private fun routeHorizontal(
        edge: DiagramEdge,
        source: NodeLayout,
        target: NodeLayout,
        parallelOffset: Int
    ): EdgeLayout {

        val leftToRight =
            target.x > source.x

        val startBase =
            if (leftToRight) {
                source.right
            } else {
                source.left
            }

        val endBase =
            if (leftToRight) {
                target.left
            } else {
                target.right
            }

        val start =
            Point(
                startBase.x,
                startBase.y + parallelOffset
            )

        val end =
            Point(
                endBase.x,
                endBase.y + parallelOffset
            )

        /*
         * Для горизонтального transition
         * пока оставляем подписи над линией.
         */
        val eventPosition =
            if (edge.event != null) {
                Point(
                    (start.x + end.x) / 2,
                    start.y - 12
                )
            } else {
                null
            }

        val guardPosition =
            if (edge.guard != null) {
                Point(
                    if (leftToRight) {
                        end.x - 20
                    } else {
                        end.x + 20
                    },
                    end.y - 12
                )
            } else {
                null
            }

        val actionLabelPositions =
            createActionPositions(
                edge = edge,
                target = target,
                parallelOffset = parallelOffset
            )

        return EdgeLayout(
            points = listOf(
                start,
                end
            ),
            eventLabelPosition = eventPosition,
            guardLabelPosition = guardPosition,
            actionLabelPositions = actionLabelPositions,
        )
    }

    private fun routeBackward(
        edge: DiagramEdge,
        source: NodeLayout,
        target: NodeLayout,
        parallelOffset: Int
    ): EdgeLayout {

        val start =
            Point(
                source.right.x,
                source.right.y + parallelOffset
            )

        val end =
            Point(
                target.right.x,
                target.right.y + parallelOffset
            )

        /*
         * Back edge выводим за правую
         * часть соответствующих nodes.
         */
        val sideX =
            maxOf(
                source.right.x,
                target.right.x
            ) +
                    BACK_EDGE_GAP +
                    parallelOffset

        val firstCorner =
            Point(
                sideX,
                start.y
            )

        val secondCorner =
            Point(
                sideX,
                end.y
            )

        val eventPosition =
            if (edge.event != null) {
                Point(
                    sideX,
                    (start.y + end.y) / 2
                )
            } else {
                null
            }

        val guardPosition =
            if (edge.guard != null) {
                Point(
                    target.right.x,
                    target.y - GUARD_TARGET_OFFSET_Y
                )
            } else {
                null
            }

        val actionLabelPositions =
            createActionPositions(
                edge = edge,
                target = target,
                parallelOffset = parallelOffset
            )

        return EdgeLayout(
            points = listOf(
                start,
                firstCorner,
                secondCorner,
                end
            ),
            eventLabelPosition = eventPosition,
            guardLabelPosition = guardPosition,
            actionLabelPositions = actionLabelPositions,
        )
    }

    /**
     * Event привязываем к target.
     *
     * Получаем:
     *
     *       EVENT
     *         |
     *       GUARD
     *         v
     *   +-----------+
     *   |  TARGET   |
     *   +-----------+
     */
    private fun createEventPosition(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): Point? {

        if (edge.event == null) {
            return null
        }

        return Point(
            target.center.x + parallelOffset,
            target.top.y - EVENT_TARGET_OFFSET_Y
        )
    }

    /**
     * Guard располагается непосредственно
     * перед target.
     *
     * Для choice это особенно важно:
     *
     *              CHOICE
     *                ◇
     *          /     |     \
     *         /      |      \
     *
     * modernGuard usedGuard oldGuard
     *      v        v        v
     *   MODERN     USED      OLD
     */
    private fun createGuardPosition(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): Point? {

        /*
         * У обычного transition без guard
         * ничего не рисуем.
         *
         * У CHOICE_BRANCH без guard
         * SsmDiagramPanel покажет "otherwise".
         */
        if (
            edge.guard == null &&
            edge.type != DiagramEdge.Type.CHOICE_BRANCH
        ) {
            return null
        }

        return Point(
            target.center.x + parallelOffset,
            target.top.y - GUARD_TARGET_OFFSET_Y
        )
    }

    private data class EdgePair(
        val source: String,
        val target: String
    )
}