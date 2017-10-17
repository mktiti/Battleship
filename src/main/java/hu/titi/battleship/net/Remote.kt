package hu.titi.battleship.net

import hu.titi.battleship.model.Coordinate
import hu.titi.battleship.model.Ship
import hu.titi.battleship.ui.GameView
import hu.titi.battleship.ui.TileState

class Remote(private val isHost: Boolean, private val host: GameHost, private val view: GameView) : GameView {

    override fun gameOver(won: Boolean) {
        if (!isHost) {
            host.gameOver(won)
        }
    }

    override fun updateTile(position: Coordinate, state: TileState): Boolean {
        view.updateTile(position, state)
        return host.updateTile(isHost, position, state)
    }

    override fun showShips(over: Boolean, ships: Collection<Ship>): Boolean = when {
        over -> {
            host.showShips(isHost, ships)
            view.showShips(over, ships)
        }
        isHost -> view.showShips(over, ships)
        else -> {
            host.showShips(isHost, ships)
            true
        }
    }

    override fun unveilShip(ship: Ship): Boolean {
        view.unveilShip(ship)
        return host.unveilShip(isHost, ship)
    }
}