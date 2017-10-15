package hu.titi.battleship.model

import android.util.Log
import java.io.Serializable

val shipSizes = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

private const val TAG = "ship"

class Ship(private val length: Int, private val position: Coordinate, private val vertical: Boolean) : Serializable {

    var sunk = false

    // (Position, isHit)
    val tiles = Array(length) { i ->
        val dx = if (vertical) 0 else 1
        val dy = 1 - dx

        Pair(Coordinate(position.x + dx * i, position.y + dy * i), false)
    }

    fun occupies(position: Coordinate) = tiles.map(Pair<Coordinate, Boolean>::first).contains(position)

    fun hit(position: Coordinate): ShootResult {
        val indexed = tiles.withIndex().asSequence().find { (_, p) -> p.first == position } ?: return ShootResult.MISS

        tiles[indexed.index] = Pair(indexed.value.first, true)
        sunk = tiles.asSequence().all(Pair<Coordinate, Boolean>::second)
        return if (sunk) ShootResult.SINK else ShootResult.HIT
    }

    fun border(): List<Coordinate> = mutableListOf<Coordinate>().apply {
        for (dNarrow in -1..1) {
            for (dWide in -1..length) {
                val x = position.x + if (vertical) dNarrow else dWide
                val y = position.y + if (vertical) dWide else dNarrow

                if (validCoordinate(x, y)) {
                    val coord = Coordinate(x, y)
                    if (coord !in tiles.map(Pair<Coordinate, *>::first)) add(0, coord)
                }
            }
        }
    }

    override fun toString() = "{$length;$position;$vertical}"
}

fun parseShip(string: String): Ship? {
    val ss = string.replace("{", "").replace("}", "").split(";")
    try {
        return Ship(ss[0].toInt(), parseSafe(ss[1]) ?: return null, ss[2].toBoolean())
    } catch (nfe: NumberFormatException) {
        Log.i(TAG, "number format exception")
        return null
    } catch (aie: ArrayIndexOutOfBoundsException) {
        Log.i(TAG, "array index out of bounds")
        return null
    }
}