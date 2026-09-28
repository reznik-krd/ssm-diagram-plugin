package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import java.awt.Point
import kotlin.math.abs

/**
 * Routes diagram edges as readable orthogonal polylines between laid-out nodes.
 */
class OrthogonalEdgeRouter {

    companion object {

        /**
         * Distance between parallel transitions.
         */
        private const val PARALLEL_EDGE_GAP = 18

        /**
         * Event position relative to target.
         */
        private const val EVENT_TARGET_OFFSET_Y = 42

        /**
         * Distance used for backward edges.
         */
        private const val BACK_EDGE_GAP = 70

        /**
         * Padding around nodes used by obstacle-aware routing.
         */
        private const val NODE_OBSTACLE_PADDING = 24

        /**
         * Distance between the last action and target state.
         */
        private const val ACTION_TARGET_OFFSET_Y = 30

        /**
         * Distance between multiple actions.
         */
        private const val ACTION_LABEL_GAP = 24

        /**
         * Distance between guard and the first action.
         */
        private const val GUARD_ACTION_GAP = 5

        /**
         * Distance between event and guard/action stack.
         */
        private const val EVENT_GUARD_GAP = 30
    }

    /**
     * Calculates orthogonal geometry for all graph edges.
     */
    fun route(
        graph: DiagramGraph,
        nodes: Map<String, NodeLayout>
    ): Map<String, EdgeLayout> {

        val result =
            mutableMapOf<String, EdgeLayout>()

        /*
         * Group transitions with the same source and target.
         *
         * Example:
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

                val edgeLayout =
                    routeEdge(
                        edge = edge,
                        source = source,
                        target = target,
                        parallelOffset = parallelOffset
                    )

                /*
                 * After the normal route is built,
                 * make sure its segments do not pass
                 * through foreign nodes.
                 */
                val safePoints =
                    avoidNodeIntersections(
                        edge = edge,
                        points = edgeLayout.points,
                        nodes = nodes
                    )

                result[edge.id] =
                    edgeLayout.copy(
                        points = safePoints
                    )
            }
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Obstacle-aware routing
    // -------------------------------------------------------------------------

    private fun avoidNodeIntersections(
        edge: DiagramEdge,
        points: List<Point>,
        nodes: Map<String, NodeLayout>
    ): List<Point> {

        if (points.size < 2) {
            return points
        }

        /*
         * Source and target are intentionally excluded.
         *
         * An edge naturally starts/ends on them.
         */
        val obstacles =
            nodes
                .filterKeys { nodeId ->
                    nodeId != edge.source &&
                            nodeId != edge.target
                }
                .values
                .toList()

        if (obstacles.isEmpty()) {
            return points
        }

        val result =
            mutableListOf<Point>()

        result += points.first()

        points
            .zipWithNext()
            .forEach { (start, end) ->

                val replacement =
                    routeSegmentAroundObstacles(
                        start = start,
                        end = end,
                        obstacles = obstacles
                    )

                replacement
                    .drop(1)
                    .forEach { point ->
                        result += point
                    }
            }

        return simplifyPoints(result)
    }

    private fun routeSegmentAroundObstacles(
        start: Point,
        end: Point,
        obstacles: List<NodeLayout>
    ): List<Point> {

        if (start.x == end.x) {
            return routeVerticalSegmentAroundObstacles(
                start = start,
                end = end,
                obstacles = obstacles
            )
        }

        if (start.y == end.y) {
            return routeHorizontalSegmentAroundObstacles(
                start = start,
                end = end,
                obstacles = obstacles
            )
        }

        /*
         * Our router normally produces orthogonal segments,
         * but leave a safe fallback.
         */
        return listOf(
            start,
            end
        )
    }

    private fun routeVerticalSegmentAroundObstacles(
        start: Point,
        end: Point,
        obstacles: List<NodeLayout>
    ): List<Point> {

        val minY =
            minOf(
                start.y,
                end.y
            )

        val maxY =
            maxOf(
                start.y,
                end.y
            )

        val blockingNodes =
            obstacles.filter { node ->

                val left =
                    node.x - NODE_OBSTACLE_PADDING

                val right =
                    node.x +
                            node.width +
                            NODE_OBSTACLE_PADDING

                val top =
                    node.y - NODE_OBSTACLE_PADDING

                val bottom =
                    node.y +
                            node.height +
                            NODE_OBSTACLE_PADDING

                start.x in left..right &&
                        maxY >= top &&
                        minY <= bottom
            }

        if (blockingNodes.isEmpty()) {
            return listOf(
                start,
                end
            )
        }

        /*
         * Build one common corridor around all nodes
         * blocking this vertical segment.
         */
        val leftCorridor =
            blockingNodes.minOf { node ->
                node.x
            } - NODE_OBSTACLE_PADDING

        val rightCorridor =
            blockingNodes.maxOf { node ->
                node.x + node.width
            } + NODE_OBSTACLE_PADDING

        /*
         * Choose the nearest side.
         */
        val corridorX =
            if (
                abs(start.x - leftCorridor) <
                abs(start.x - rightCorridor)
            ) {
                leftCorridor
            } else {
                rightCorridor
            }

        return listOf(
            start,
            Point(
                corridorX,
                start.y
            ),
            Point(
                corridorX,
                end.y
            ),
            end
        )
    }

    private fun routeHorizontalSegmentAroundObstacles(
        start: Point,
        end: Point,
        obstacles: List<NodeLayout>
    ): List<Point> {

        val minX =
            minOf(
                start.x,
                end.x
            )

        val maxX =
            maxOf(
                start.x,
                end.x
            )

        val blockingNodes =
            obstacles.filter { node ->

                val left =
                    node.x - NODE_OBSTACLE_PADDING

                val right =
                    node.x +
                            node.width +
                            NODE_OBSTACLE_PADDING

                val top =
                    node.y - NODE_OBSTACLE_PADDING

                val bottom =
                    node.y +
                            node.height +
                            NODE_OBSTACLE_PADDING

                start.y in top..bottom &&
                        maxX >= left &&
                        minX <= right
            }

        if (blockingNodes.isEmpty()) {
            return listOf(
                start,
                end
            )
        }

        val topCorridor =
            blockingNodes.minOf { node ->
                node.y
            } - NODE_OBSTACLE_PADDING

        val bottomCorridor =
            blockingNodes.maxOf { node ->
                node.y + node.height
            } + NODE_OBSTACLE_PADDING

        val corridorY =
            if (
                abs(start.y - topCorridor) <
                abs(start.y - bottomCorridor)
            ) {
                topCorridor
            } else {
                bottomCorridor
            }

        return listOf(
            start,
            Point(
                start.x,
                corridorY
            ),
            Point(
                end.x,
                corridorY
            ),
            end
        )
    }

    private fun simplifyPoints(
        points: List<Point>
    ): List<Point> {

        if (points.size <= 2) {
            return points
        }

        val result =
            mutableListOf<Point>()

        points.forEach { point ->

            /*
             * Remove duplicate points.
             */
            if (
                result.isNotEmpty() &&
                result.last() == point
            ) {
                return@forEach
            }

            if (result.size < 2) {
                result += point
                return@forEach
            }

            val first =
                result[result.size - 2]

            val second =
                result[result.size - 1]

            val sameVerticalLine =
                first.x == second.x &&
                        second.x == point.x

            val sameHorizontalLine =
                first.y == second.y &&
                        second.y == point.y

            if (
                sameVerticalLine ||
                sameHorizontalLine
            ) {
                result[result.lastIndex] =
                    point
            } else {
                result += point
            }
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Parallel edges
    // -------------------------------------------------------------------------

    private fun calculateParallelOffset(
        index: Int,
        count: Int
    ): Int {

        if (count <= 1) {
            return 0
        }

        /*
         * Two:
         *
         * -9
         * +9
         *
         * Three:
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

    // -------------------------------------------------------------------------
    // Routing
    // -------------------------------------------------------------------------

    private fun routeEdge(
        edge: DiagramEdge,
        source: NodeLayout,
        target: NodeLayout,
        parallelOffset: Int
    ): EdgeLayout {

        /*
         * Normal layered transition:
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
         * Same layer:
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
         * Cycle / backward transition.
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
         * Source and target are directly
         * above/below each other.
         */
        if (start.x == end.x) {

            return EdgeLayout(
                points = listOf(
                    start,
                    end
                ),
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
         * For a horizontal transition keep the event
         * above the line.
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

        /*
         * Actions and guard still form a stack
         * immediately above target.
         */
        return EdgeLayout(
            points = listOf(
                start,
                end
            ),
            eventLabelPosition = eventPosition,
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
         * Back edge initially goes outside
         * source/target.
         *
         * Obstacle-aware routing may modify
         * these segments afterwards.
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

        return EdgeLayout(
            points = listOf(
                start,
                firstCorner,
                secondCorner,
                end
            ),
            eventLabelPosition = eventPosition,
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

    // -------------------------------------------------------------------------
    // Labels
    // -------------------------------------------------------------------------

    /**
     * Actions are placed immediately before the target state.
     *
     * Visual order:
     *
     * ACTION 1
     * ACTION 2
     * ACTION 3
     *    |
     * TARGET
     */
    private fun createActionPositions(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): List<Point> {

        if (edge.actions.isEmpty()) {
            return emptyList()
        }

        val actionsCount =
            edge.actions.size

        return edge.actions.mapIndexed { index, _ ->

            /*
             * Last action is closest to target.
             */
            val distanceFromTarget =
                ACTION_TARGET_OFFSET_Y +
                        (actionsCount - 1 - index) *
                        ACTION_LABEL_GAP

            Point(
                target.center.x + parallelOffset,
                target.top.y - distanceFromTarget
            )
        }
    }

    /**
     * Guard is placed above all actions.
     *
     * GUARD
     *
     * ACTION 1
     * ACTION 2
     *
     * TARGET
     */
    private fun createGuardPosition(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): Point? {

        /*
         * Normal transition without guard:
         * nothing is displayed.
         *
         * CHOICE_BRANCH without guard:
         * renderer displays "otherwise".
         */
        val hasGuard =
            edge.guard != null ||
                    edge.type == DiagramEdge.Type.CHOICE_BRANCH

        if (!hasGuard) {
            return null
        }

        val actionsHeight =
            edge.actions.size *
                    ACTION_LABEL_GAP

        return Point(
            target.center.x + parallelOffset,
            target.top.y -
                    ACTION_TARGET_OFFSET_Y -
                    actionsHeight -
                    GUARD_ACTION_GAP
        )
    }

    /**
     * Event is placed above the whole
     * guard + actions stack.
     *
     * EVENT
     *
     * GUARD
     *
     * ACTION
     *
     * TARGET
     */
    private fun createEventPosition(
        edge: DiagramEdge,
        target: NodeLayout,
        parallelOffset: Int
    ): Point? {

        if (edge.event == null) {
            return null
        }

        val actionsHeight =
            edge.actions.size *
                    ACTION_LABEL_GAP

        val guardHeight =
            if (
                edge.guard != null ||
                edge.type == DiagramEdge.Type.CHOICE_BRANCH
            ) {
                GUARD_ACTION_GAP
            } else {
                0
            }

        /*
         * EVENT_TARGET_OFFSET_Y provides the basic
         * distance from target.
         *
         * Additional height is added for actions
         * and guard so labels cannot occupy
         * the same position.
         */
        return Point(
            target.center.x + parallelOffset,
            target.top.y -
                    EVENT_TARGET_OFFSET_Y -
                    actionsHeight -
                    guardHeight -
                    EVENT_GUARD_GAP
        )
    }

    private data class EdgePair(
        val source: String,
        val target: String
    )
}