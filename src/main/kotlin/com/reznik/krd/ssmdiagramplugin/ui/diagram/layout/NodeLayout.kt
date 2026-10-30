package com.reznik.krd.ssmdiagramplugin.ui.diagram.layout

import java.awt.Point

/**
 * Хранит координаты и размеры узла, а также его точки подключения.
 */
data class NodeLayout(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {

    val top: Point
        get() = Point(x + width / 2, y)

    val bottom: Point
        get() = Point(x + width / 2, y + height)

    val left: Point
        get() = Point(x, y + height / 2)

    val right: Point
        get() = Point(x + width, y + height / 2)

    val center: Point
        get() = Point(x + width / 2, y + height / 2)
}