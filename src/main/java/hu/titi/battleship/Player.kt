package hu.titi.battleship

interface PlayerListener {
    //fun enable()
    //fun disable()
    fun await(prevResult: ShootResult): Coordinate
}

class Player(val model: Map, val listener: PlayerListener, val view: GameView, val showShips: Boolean = false)