package hu.titi.battleship.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import org.jetbrains.anko.runOnUiThread
import java.io.Serializable

private const val X_BLOCKS = 10
private const val Y_BLOCKS = 9
private const val SHIP_NUMBER = 10

private const val TAG = "ship-view"

class ShipView(context: Context, attributes: AttributeSet) : View(context, attributes) {

    private val availablePaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.SHIP.color
    }
    private val usedPaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.HIT.color
    }
    private val selectedPaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.MISS.color
    }

    private var selected: Pair<Int, Int>? = null
    private var used = mutableListOf<Pair<Int, Int>>()

    init {
        setOnTouchListener { _, motionEvent ->
            select(motionEvent)
            true
        }
    }

    private fun select(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP) {
            val sizeX = width / X_BLOCKS
            val sizeY = height / Y_BLOCKS

            val size = minOf(sizeX, sizeY)

            val startX = (width - X_BLOCKS * size) / 2F
            val startY = (height - Y_BLOCKS * size) / 2F

            val x = (event.x - startX).toInt()
            val y = (event.y - startY).toInt()

            Log.i(TAG, "X: $x, Y: $y")

            if (x > X_BLOCKS * size || y > Y_BLOCKS * size) return

            val col = minOf(x / size, Y_BLOCKS)
            val row = minOf(y / size, X_BLOCKS)

            Log.i(TAG, "row: $row, col: $col")

            if (row % 2 == 0) return
            val posY = row / 2

            Log.i(TAG, "pos y: $posY")

            val width = 4 - posY
            if (col % (width + 1) == 0) return
            val posX = col / (width + 1)
            if (posX > posY + 1) return

            Log.i(TAG, "pos x: $posX")

            val pos = Pair(posX + 1, posY + 1)
            if (pos !in used) {
                selected = pos
                invalidate()
            }
        }
    }

    fun save(bundle: Bundle) {
        bundle.putSerializable("selected", selected as Serializable?)
        bundle.putSerializable("used", used as Serializable)
    }

    @Suppress("UNCHECKED_CAST")
    fun fromSaved(bundle: Bundle) {
        selected = bundle.getSerializable("selected") as? Pair<Int, Int>
        val usedSave = bundle.getSerializable("used")
        if (usedSave != null && usedSave is List<*>) {
            val usedShips: List<Pair<Int, Int>> = (usedSave as List<*>).filterIsInstance<Pair<Int, Int>>()
            used.clear()
            used.addAll(usedShips)
        }
        invalidate()
    }

    fun undo() {
        if (used.isNotEmpty()) {
            used.removeAt(used.size - 1)
            invalidate()
        }
    }

    fun allUsed() {
        selected = null

        used.clear()
        for (y in 1..4) {
            for (x in 1..y) {
                used.add(Pair(x, y))
            }
        }

        invalidate()
    }

    fun getSelected(): Int? {
        val s = selected
        return if (s == null) null else 5 - s.second
    }

    fun removeSelected(): Boolean {
        selected?.let {
            used.add(it)
        }
        selected = null
        context.runOnUiThread {
            invalidate()
        }

        return used.size == SHIP_NUMBER
    }

    override fun onDraw(canvas: Canvas) {
        val sizeX = width / X_BLOCKS
        val sizeY = height / Y_BLOCKS

        val size = minOf(sizeX, sizeY)

        val startX = (width - X_BLOCKS * size) / 2F
        val startY = (height - Y_BLOCKS * size) / 2F

        for (y in 1..4) {
            val width = 5 - y
            for (x in 1..y) {
                val posX = size + startX + (x - 1) * (width + 1) * size
                val posY = size + startY + (y - 1) * 2 * size

                val coordinate = Pair(x, y)
                val paint = when {
                    selected == coordinate -> selectedPaint
                    coordinate in used -> usedPaint
                    else -> availablePaint
                }

                canvas.drawRect(posX, posY, (posX + width * size), (posY + size), paint)
            }
        }

    }

}