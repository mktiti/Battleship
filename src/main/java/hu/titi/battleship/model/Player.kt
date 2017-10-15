package hu.titi.battleship.model

import hu.titi.battleship.ui.GameView

interface PlayerListener {
    //fun enable()
    //fun disable()
    fun await(prevResult: ShootResult): Coordinate?
    fun abort()
}

class Player(val model: Map, val listener: PlayerListener, val view: GameView)