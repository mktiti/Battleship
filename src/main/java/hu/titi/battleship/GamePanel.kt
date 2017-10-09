package hu.titi.battleship

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk23.listeners.onClick
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface GameView {
    fun updateTile(position: Coordinate, state: TileState)
    fun showShips(ships: Collection<Ship>)
    fun unveilShip(ship: Ship)
}

interface CoordinateStore {
    fun place(coordinate: Coordinate)
    fun await(): Coordinate
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
    private val rows = Array(SIZE + 1, this::createRow)

    private var enabled = AtomicBoolean(false)
    private val store = LocalCoordinateStore()

    init {
        orientation = VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, matchParent, 1F)
        weightSum = (SIZE + 1).toFloat()
    }

    private fun createRow(row: Int) = linearLayout {
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

    private fun tileClick(coordinate: Coordinate) {
        if (enabled.get()) {
            store.place(coordinate)
        }
    }

    override fun showShips(ships: Collection<Ship>) {
        context.runOnUiThread {
            for (ship in ships) {
                for (coord in ship.tiles.map(Pair<Coordinate, Boolean>::first)) {
                    this@GamePanel[coord].state = TileState.SHIP
                }
            }
        }
    }

    override fun await(prevResult: ShootResult): Coordinate {
        enabled.set(true)
        val coord = store.await()
        enabled.set(false)
        return coord
    }

    override fun unveilShip(ship: Ship) {
        context.runOnUiThread {
            ship.border().forEach {
                this@GamePanel[it].state = TileState.MISS
            }
        }
    }

    override fun updateTile(position: Coordinate, state: TileState) {
        context.runOnUiThread {
            this@GamePanel[position].state = state
        }
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

private class LocalCoordinateStore : CoordinateStore {
    private val store = ArrayBlockingQueue<Coordinate>(1)

    override fun place(coordinate: Coordinate) {
        store.offer(coordinate)
    }

    override fun await(): Coordinate = store.take()
}
