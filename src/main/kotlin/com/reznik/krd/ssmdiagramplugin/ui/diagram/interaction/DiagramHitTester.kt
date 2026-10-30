package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.reznik.krd.ssmdiagramplugin.model.SsmAction
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import java.awt.Point
import kotlin.math.sqrt

/** Находит интерактивные элементы по экранным координатам с учётом масштаба. */
internal class DiagramHitTester(
    private val graphProvider: () -> DiagramGraph?,
    private val layoutProvider: () -> DiagramLayout?,
    private val zoomProvider: () -> Double,
    private val hitRegistry: DiagramHitRegistry
) {
    private val graph get() = graphProvider()
    private val layout get() = layoutProvider()
    private val zoom get() = zoomProvider()

    /** Находит состояние под указателем. */
    fun findNodeAt(
        mouseX: Int,
        mouseY: Int
    ): DiagramNode? {
        val currentGraph = graph ?: return null
        val currentLayout = layout ?: return null
        val diagramX = (mouseX / zoom).toInt()
        val diagramY = (mouseY / zoom).toInt()

        return currentGraph.nodes
            .firstOrNull { node ->
                val nodeLayout = currentLayout.nodes[node.id] ?: return@firstOrNull false

                diagramX >= nodeLayout.x &&
                        diagramX <=
                        nodeLayout.x +
                        nodeLayout.width &&
                        diagramY >= nodeLayout.y &&
                        diagramY <=
                        nodeLayout.y +
                        nodeLayout.height
            }
    }

    /** Находит область подписи действия. */
    fun findActionAt(mouseX: Int, mouseY: Int): ActionHitArea? {
        val diagramX = (mouseX / zoom).toInt()
        val diagramY = (mouseY / zoom).toInt()

        return hitRegistry.actionAreas
            .lastOrNull { area -> area.bounds.contains(diagramX, diagramY)
            }
    }

    /** Получает действие из области попадания. */
    fun findAction(area: ActionHitArea): SsmAction? {
        val currentGraph = graph ?: return null

        val edge = currentGraph.edges
                .firstOrNull { it.id == area.edgeId }
                ?: return null

        return edge.actions
            .getOrNull(area.actionIndex)
    }

    /** Находит переход по подписи события. */
    fun findEventEdgeAt(mouseX: Int, mouseY: Int): DiagramEdge? {
        val currentGraph = graph ?: return null
        val diagramX = (mouseX / zoom).toInt()
        val diagramY = (mouseY / zoom).toInt()

        val edgeId = hitRegistry.eventAreas.entries
                .lastOrNull { (_, bounds) ->
                    bounds.contains(
                        diagramX,
                        diagramY
                    )
                }?.key ?: return null

        return currentGraph.edges.firstOrNull { it.id == edgeId }
    }

    /** Находит переход по подписи guard. */
    fun findGuardEdgeAt(mouseX: Int, mouseY: Int): DiagramEdge? {
        val currentGraph = graph ?: return null
        val diagramX = (mouseX / zoom).toInt()
        val diagramY = (mouseY / zoom).toInt()

        val edgeId = hitRegistry.guardAreas.entries
                .lastOrNull { (_, bounds) ->
                    bounds.contains(
                        diagramX,
                        diagramY
                    )
                }?.key ?: return null

        return currentGraph.edges.firstOrNull { it.id == edgeId }
    }

    /** Находит ближайший переход в пределах допуска. */
    fun findDiagramEdgeAt(mouseX: Int, mouseY: Int): DiagramEdge? {
        val currentGraph = graph ?: return null
        val currentLayout = layout ?: return null
        val diagramX = mouseX / zoom
        val diagramY = mouseY / zoom

        val tolerance = DiagramInteractionConstants.EDGE_CLICK_TOLERANCE / zoom

        return currentGraph.edges
            .mapNotNull { edge ->
                val edgeLayout = currentLayout.edges[edge.id] ?: return@mapNotNull null

                val distance =
                    edgeLayout.points
                        .zipWithNext()
                        .minOfOrNull { (start, end) ->
                            distanceToSegment(
                                px = diagramX,
                                py = diagramY,
                                start = start,
                                end = end
                            )
                        }
                        ?: return@mapNotNull null

                edge to distance
            }
            .filter { (_, distance) -> distance <= tolerance }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    /** Вычисляет расстояние от точки до отрезка. */
    private fun distanceToSegment(
        px: Double,
        py: Double,
        start: Point,
        end: Point
    ): Double {
        val dx = (end.x - start.x).toDouble()
        val dy = (end.y - start.y).toDouble()

        if (dx == 0.0 && dy == 0.0) {
            val pointDx = px - start.x
            val pointDy = py - start.y
            return sqrt(pointDx * pointDx + pointDy * pointDy)
        }

        val t = ((px - start.x) * dx + (py - start.y) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0.0, 1.0)
        val closestX = start.x + clampedT * dx
        val closestY = start.y + clampedT * dy
        val distanceX = px - closestX
        val distanceY = py - closestY

        return sqrt(distanceX * distanceX + distanceY * distanceY)
    }

}
