package hu.titi.battleship.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import hu.titi.battleship.model.*
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.sdk23.listeners.onClick
import org.jetbrains.anko.textView
import java.util.concurrent.atomic.AtomicBoolean

interface GameView {
    fun updateTile(position: Coordinate, state: TileState): Boolean
    fun showShips(ships: Collection<Ship>): Boolean
    fun unveilShip(ship: Ship): Boolean
    fun gameOver(won: Boolean)
}

private typealias Generator = LinearLayout.(Int, Int) -> View

private val firstRowGenerator: Generator = { x, _ ->
    textView(x.toString()) {
        gravity = Gravity.CENTER
    }
}

class GamePanel(context: Context) : LinearLayout(context), GameView, PlayerListener {

    private val gameTiles = Array(SIZE) { y ->
        Array(SIZE) { x ->
            GameTile(context, Coordinate(x, y), TileState.UNKNOWN).apply {
                onClick {
                    tileClick(coordinate)
                }
            }
        }
    }
    //private val rows = Array(SIZE + 1, this::createRow)

    private var enabled = AtomicBoolean(false)
    private val store = Store<Coordinate>()

    init {
        (0..SIZE).forEach(this::createRow)
        orientation = VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, matchParent, 1F)
        weightSum = (SIZE + 1).toFloat()
    }

    private fun createRow(row: Int) {
        linearLayout {
            layoutParams = LinearLayout.LayoutParams(matchParent, 0, 1F)
            weightSum = (SIZE + 1).toFloat()

            val cellParams = LinearLayout.LayoutParams(0, matchParent, 1F)

            val header = if (row == 0) "" else ('A' + row - 1).toString()
            val generator = if (row == 0) firstRowGenerator else rowGenerator()

            textView(header) {
                layoutParams = cellParams
                gravity = Gravity.CENTER
            }

            for (cell in 1..SIZE) {
                generator(cell, row).apply {
                    layoutParams = cellParams
                }
            }
        }
    }

    private fun tileClick(coordinate: Coordinate) {
        if (enabled.get()) {
            store.place(coordinate)
        }
    }

    override fun showShips(ships: Collection<Ship>): Boolean {
        context.runOnUiThread {
            ships.flatMap { it.tiles.map(Pair<Coordinate, Boolean>::first) }
                 .forEach { this@GamePanel[it].state = TileState.SHIP }
        }

        return true
    }

    override fun gameOver(won: Boolean) {}

    override fun abort() {
        store.place(Coordinate(0, 0))
    }

    override fun await(prevResult: ShootResult): Coordinate {
        enabled.set(true)
        val coord = store.await()
        enabled.set(false)
        return coord
    }

    override fun unveilShip(ship: Ship): Boolean {
        context.runOnUiThread {
            ship.border().forEach {
                this@GamePanel[it].state = TileState.MISS
            }
        }

        return true
    }

    override fun updateTile(position: Coordinate, state: TileState): Boolean {
        context.runOnUiThread {
            this@GamePanel[position].state = state
        }

        return true
    }

    private fun LinearLayout.rowGenerator(): Generator = { x, y ->
        this@GamePanel[x - 1, y - 1].apply {
            this@rowGenerator.addView(this)
        }
    }

    private operator fun get(position: Coordinate) = gameTiles[position.y][position.x]

    private operator fun get(x: Int, y: Int) = get(Coordinate(x, y))

    /*
    override fun enable() {
        enabled.set(true)
    }

    override fun disable() {
        enabled.set(false)
    }
    */
}
