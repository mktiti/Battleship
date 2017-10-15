package hu.titi.battleship.model

import android.util.Log
import java.io.Serializable

data class Coordinate(val x: Int = 0, val y: Int = 0) : Serializable {
    constructor(pair: Pair<Int, Int>) : this(pair.first, pair.second)

    init {
        if (!validCoordinate(x, y)) {
            throw IllegalArgumentException("Index must be between 0 and ${SIZE}")
        }
    }

    operator fun compareTo(coordinate: Coordinate) = (x - coordinate.x) + (y - coordinate.y)

    override fun toString() = "($x,$y)"
}

fun validCoordinate(x: Int, y: Int) = x in 0 until SIZE && y in 0 until SIZE

fun validCoordinate(pair: Pair<Int, Int>) = validCoordinate(pair.first, pair.second)

fun parseSafe(string: String): Coordinate? {

    val ss = string.replace("(", "").replace(")", "").split(",")
    if (ss.size != 2) return null

    try {
        val x = ss[0].toInt()
        val y = ss[1].toInt()
        if (validCoordinate(x, y)) return Coordinate(x, y)
    } catch (nfe: NumberFormatException) {
        Log.i("coordinate", "NumberFormatException while parsing coordinate")
        Log.i("coordinate", nfe.message)
    }

    return null
}