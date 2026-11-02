package com.reznik.krd.ssmdiagramplugin.export

import com.reznik.krd.ssmdiagramplugin.ui.diagram.DiagramDisplayOptions
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramHitRegistry
import com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction.DiagramInteractionState
import com.reznik.krd.ssmdiagramplugin.ui.diagram.layout.DiagramLayout
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph
import com.reznik.krd.ssmdiagramplugin.ui.diagram.render.DiagramRenderer
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import java.awt.Color
import java.awt.Dimension
import java.awt.RenderingHints
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Экспортирует текущую геометрию диаграммы в векторный SVG без Zoom, Pan и выделения. */
class SvgDiagramExporter {

    /** Сохраняет всю диаграмму, учитывая текущую видимость Guards и Actions. */
    fun export(
        destination: Path,
        graph: DiagramGraph,
        layout: DiagramLayout,
        options: DiagramDisplayOptions
    ) {
        val document = GenericDOMImplementation.getDOMImplementation()
            .createDocument(SVG_NAMESPACE, "svg", null)
        val svg = SVGGraphics2D(document)
        val width = layout.size.width.coerceAtLeast(1)
        val height = layout.size.height.coerceAtLeast(1)
        svg.svgCanvasSize = Dimension(width, height)

        // Экспорт всегда с чистым контекстом взаимодействия: без hover, selection и focus.
        val renderer = DiagramRenderer(DiagramInteractionState(), DiagramHitRegistry())
        val graphics = svg.create() as java.awt.Graphics2D
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, width, height)
            renderer.render(graphics, graph, layout, options)
        } finally {
            graphics.dispose()
        }

        Files.newBufferedWriter(destination, StandardCharsets.UTF_8).use { writer ->
            svg.stream(writer, true)
        }
    }

    private companion object {
        const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
    }
}
