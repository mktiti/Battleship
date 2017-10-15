package hu.titi.battleship.model

import android.util.Log
import java.io.Serializable

class Coordinate private constructor(val x: Int, val y: Int) : Serializable {

    companion object {
        private val coordinateTable = (0 until SIZE).flatMap { x -> (0 until SIZE).map { y -> Coordinate(x, y) } }.toList()

        fun of(pair: Pair<Int, Int>) = of(pair.first, pair.second)

        fun of(x: Int = 0, y: Int = 0): Coordinate {
            if (!validCoordinate(x, y)) {
                throw IllegalArgumentException("Index must be between 0 and $SIZE")
            }
            return coordinateTable[x * SIZE + y]
        }
    }

    operator fun compareTo(coordinate: Coordinate) = (x - coordinate.x) + (y - coordinate.y)

    override fun toString() = "($x,$y)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coordinate

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }
}

fun validCoordinate(x: Int, y: Int) = x in 0 until SIZE && y in 0 until SIZE

fun validCoordinate(pair: Pair<Int, Int>) = validCoordinate(pair.first, pair.second)

fun parseSafe(string: String): Coordinate? {

    val ss = string.replace("(", "").replace(")", "").split(",")
    if (ss.size != 2) return null

    try {
        val x = ss[0].toInt()
        val y = ss[1].toInt()
        if (validCoordinate(x, y)) return Coordinate.of(x, y)
    } catch (nfe: NumberFormatException) {
        Log.i("coordinate", "NumberFormatException while parsing coordinate")
        Log.i("coordinate", nfe.message)
    }

    return null
}