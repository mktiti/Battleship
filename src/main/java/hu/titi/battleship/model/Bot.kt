package hu.titi.battleship.model

import android.util.Log
import java.util.*

private val deltas = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))

class Bot(private val opponentMap: Map) : PlayerListener {

    private val random = Random()

    private val targetShip: MutableList<Coordinate> = mutableListOf()
    private var previous: Coordinate? = null

    override fun await(prevResult: ShootResult): Coordinate {
        Thread.sleep(800)

        val target = when {
            prevResult == ShootResult.SINK -> {
                targetShip.clear()
                randomShoot()
            }
            (targetShip.isEmpty() && prevResult == ShootResult.HIT) ||
                    (targetShip.size == 1 && prevResult == ShootResult.MISS) -> secondShipHit(prevResult)

            targetShip.isNotEmpty() -> nextShipHit(prevResult)
            else -> randomShoot()
        }

        previous = target
        return target
    }

    override fun abort() {}

    private fun randomShoot() = opponentMap.availableTargets().toList().let {
        it[random.nextInt(it.size)]
    }

    private fun nextShipHit(prevResult: ShootResult): Coordinate {
        if (prevResult == ShootResult.HIT) {
            targetShip.add(if (previous!! < targetShip.first()) 0 else targetShip.size, previous!!)
        }
        val vertical = targetShip[0].x == targetShip[1].x

        Log.i("vertical?" , vertical.toString())
        Log.i("ship" , targetShip.joinToString())

        val dx = if (vertical) 0 else 1
        val dy = 1 - dx

        var x = targetShip[0].x - dx
        var y = targetShip[0].y - dy

        if (validCoordinate(x, y) && !opponentMap[x, y]) {
            return Coordinate.of(x, y)
        }

        x = targetShip.last().x + dx
        y = targetShip.last().y + dy

        if (validCoordinate(x, y) && !opponentMap[x, y]) {
            return Coordinate.of(x, y)
        }

        throw RuntimeException("Boat has no unchecked sides, but not sunk")
    }

    private fun secondShipHit(prevResult: ShootResult): Coordinate {
        if (prevResult == ShootResult.HIT) {
            targetShip.add(previous!!)
        }
        return around(targetShip.first()).firstOrNull() ?: throw RuntimeException("Boat has no unchecked neighbours, but not sunk")
    }

    private fun around(position: Coordinate) = deltas.asSequence().map {
        Pair(it.first + position.x, it.second + position.y)
    }.filter(::validCoordinate).map { Coordinate.of(it) }.filterNot(opponentMap::get)

}