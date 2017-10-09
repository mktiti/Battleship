package hu.titi.battleship

data class Coordinate(val x: Int = 0, val y: Int = 0) {
    constructor(pair: Pair<Int, Int>) : this(pair.first, pair.second)

    init {
        if (!validCoordinate(x, y)) {
            throw IllegalArgumentException("Index must be between 0 and $SIZE")
        }
    }

    operator fun compareTo(coordinate: Coordinate) = (x - coordinate.x) + (y - coordinate.y)
}

fun validCoordinate(x: Int, y: Int) = x in 0 until SIZE && y in 0 until SIZE

fun validCoordinate(pair: Pair<Int, Int>) = validCoordinate(pair.first, pair.second)