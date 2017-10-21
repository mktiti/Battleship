package hu.titi.battleship.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.jetbrains.anko.sdk23.coroutines.onTouch

private const val X_BLOCKS = 10
private const val Y_BLOCKS = 9
private const val SHIP_NUMBER = 10

class ShipView(context: Context, attributes: AttributeSet) : View(context, attributes) {

    private val availablePaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.SHIP.color
    }
    private val usedPaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.MISS.color
    }
    private val selectedPaint = Paint().apply {
        style = Paint.Style.FILL
        color = TileState.MISS.color
    }

    private var selected: Pair<Int, Int>? = null
    private var used = mutableListOf<Pair<Int, Int>>()

    init {
        onTouch { _, event -> select(event) }
    }

    private fun select(event: MotionEvent) {



    }

    fun getSelected() = selected

    fun unselect() {
        selected = null
        invalidate()
    }

    fun removeSelected(): Boolean {
        selected?.let {
            used.add(it)
        }
        selected = null
        invalidate()

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

                val coord = Pair(x, y)
                val paint = when {
                    selected == coord -> selectedPaint
                    coord in used -> usedPaint
                    else -> availablePaint
                }

                canvas.drawRect(posX, posY, (posX + width * size), (posY + size), paint)
            }
        }

    }

}