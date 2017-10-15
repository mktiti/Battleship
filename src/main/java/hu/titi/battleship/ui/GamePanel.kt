package hu.titi.battleship.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.service.quicksettings.Tile
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import hu.titi.battleship.model.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk23.listeners.onClick
import org.jetbrains.anko.sdk23.listeners.onTouch
import java.util.concurrent.atomic.AtomicBoolean

interface GameView {
    fun updateTile(position: Coordinate, state: TileState): Boolean
    fun showShips(ships: Collection<Ship>): Boolean
    fun unveilShip(ship: Ship): Boolean
    fun gameOver(won: Boolean)
}

enum class TileState(val color: Int) {
    UNKNOWN(Color.WHITE),
    SHIP(Color.GRAY),
    MISS(Color.BLUE),
    HIT(Color.RED)
}

class GamePanel(context: Context) : LinearLayout(context), GameView, PlayerListener {

    private val fieldView = FieldView()
    @Volatile private var clickEnabled: Boolean = false
    private val store = Store<Coordinate>()

    inner class FieldView : View(context) {
        private val paints = Array(TileState.values().size) { i ->
            Paint().apply {
                color = TileState.values()[i].color
                style = Paint.Style.FILL
            }
        }

        init {
            layoutParams = LinearLayout.LayoutParams(0, matchParent, SIZE.toFloat())
            onTouch { v, event ->
                fieldClick(event)
                true
            }
        }

        override fun onDraw(canvas: Canvas) {
            val cellWidth = canvas.width / SIZE
            val cellHeight = canvas.height / SIZE

            for (x in 0 until SIZE) {
                for (y in 0 until SIZE) {
                    canvas.drawRect((x * cellWidth).toFloat(),
                            (y * cellHeight).toFloat(),
                            ((x + 1) * cellWidth).toFloat(),
                            ((y + 1) * cellHeight).toFloat(),
                            paints[fields[x][y].ordinal])
                }
            }

        }
    }

    val fields: Array<Array<TileState>> = Array(SIZE) { x ->
        Array(SIZE) { y ->
            TileState.UNKNOWN
        }
    }

    init {
        orientation = VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, matchParent, 1F)
        weightSum = (SIZE + 1).toFloat()

        // Header
        linearLayout {
            layoutParams = LayoutParams(matchParent, 0, 1F)
            weightSum = (SIZE + 1).toFloat()

            view().lparams(width = 0, height = matchParent, weight = 1F)
            for (i in 0 until SIZE) {
                textView(text = i.toString()) {
                    gravity = Gravity.CENTER
                }.lparams(width = 0, height = matchParent, weight = 1F)
            }
        }

        linearLayout {
            layoutParams = LayoutParams(matchParent, 0, SIZE.toFloat())

            verticalLayout {
                layoutParams = LayoutParams(0, matchParent, 1F)
                weightSum = SIZE.toFloat()

                for (i in 0 until SIZE) {
                    textView(text = ('A' + i).toString()) {
                        gravity = Gravity.CENTER
                    }.lparams(width = matchParent, height = 0, weight = 1F)
                }
            }

            addView(fieldView)

        }
    }

    private fun fieldClick(event: MotionEvent) {
        if (clickEnabled && event.action == MotionEvent.ACTION_UP) {
            val x = minOf((event.x / fieldView.width * SIZE).toInt(), SIZE - 1)
            val y = minOf((event.y / fieldView.height * SIZE).toInt(), SIZE - 1)

            store.place(Coordinate(x, y))
        }
    }

    override fun await(prevResult: ShootResult): Coordinate? {
        clickEnabled = true
        val coordinate = store.await()
        clickEnabled = false
        return coordinate
    }

    override fun abort() {}

    override fun updateTile(position: Coordinate, state: TileState): Boolean {
        this[position] = state
        context.runOnUiThread {
            Log.i("sad", "invalidating")
            fieldView.invalidate()
        }
        return true
    }

    override fun showShips(ships: Collection<Ship>): Boolean {
        context.runOnUiThread {
            ships.flatMap { it.tiles.map(Pair<Coordinate, Boolean>::first) }
                    .forEach { this@GamePanel[it] = TileState.SHIP }
            fieldView.invalidate()
        }
        return true
    }

    override fun unveilShip(ship: Ship): Boolean {
        context.runOnUiThread {
            ship.border().forEach {
                this@GamePanel[it] = TileState.MISS
            }
            fieldView.invalidate()
        }
        return true
    }

    override fun gameOver(won: Boolean) {}

    private operator fun set(position: Coordinate, value: TileState) {
        fields[position.x][position.y] = value
    }

}