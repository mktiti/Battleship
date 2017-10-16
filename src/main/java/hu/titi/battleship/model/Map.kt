package hu.titi.battleship.model

import java.io.Serializable
import java.util.*

const val SIZE = 10

enum class ShootResult { MISS, HIT, SINK }

class Map : Serializable {

    val over: Boolean
        get() = ships.all(Ship::sunk)

    private val fields = Array(SIZE) { _ -> Array(SIZE) { false } }

    val ships = randomSetup()

    fun shoot(position: Coordinate): Pair<Ship?, ShootResult> {
        val result = ships.asSequence().map { Pair(it, it.hit(position)) }.find { it.second != ShootResult.MISS } ?: Pair(null, ShootResult.MISS)

        this[position.x, position.y] = true

        if (result.second == ShootResult.SINK) {
            result.first!!.border().forEach {
                this[it] = true
            }
        }

        return result
    }

    fun availableTargets(): Sequence<Coordinate> = fields.asSequence().withIndex().flatMap { (x, array) ->
        array.asSequence().withIndex().filterNot(IndexedValue<Boolean>::value).map { (y, _) ->
            Coordinate.of(x, y)
        }
    }

    operator fun get(coordinate: Coordinate) = fields[coordinate.x][coordinate.y]

    operator fun get(x: Int, y: Int) = fields[x][y]

    private operator fun set(coordinate: Coordinate, value: Boolean) {
        fields[coordinate.x][coordinate.y] = value
    }

    private operator fun set(x: Int, y: Int, value: Boolean) {
        fields[x][y] = value
    }

}

fun canFit(field: Array<Array<Boolean>>, start: Coordinate, size: Int, vertical: Boolean): Boolean {
    for (dNarrow in -1..1) {
        for (dWide in -1..size) {
            val x = start.x + if (vertical) dNarrow else dWide
            val y = start.y + if (vertical) dWide else dNarrow

            if (validCoordinate(x, y) && field[x][y]) return false
        }
    }

    return true
}

fun randomSetup(): List<Ship> {
    val random = Random()
    val ships = mutableListOf<Ship>()
    val field = Array(SIZE) { _ -> Array(SIZE) { false } }

    for (size in shipSizes) {
        val vertical = random.nextBoolean()

        val dx = if (vertical) 0 else (size - 1)
        val dy = size - 1 - dx

        val maxX = SIZE - dx
        val maxY = SIZE - dy

        (0 until maxX).asSequence().flatMap { x ->
            (0 until maxY).asSequence().map { y ->
                Coordinate.of(x, y)
            }
        }.filter {
            canFit(field, it, size, vertical)
        }.toList().apply {
            val position = this[random.nextInt(this.size)]
            val ship = Ship(size, position, vertical)
            ships.add(ship)
            for ((coordinate, _) in ship.tiles) {
                field[coordinate.x][coordinate.y] = true
            }
        }
    }

    return ships
}