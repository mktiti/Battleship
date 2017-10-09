package hu.titi.battleship

import hu.titi.battleship.activity.GameActivity

class GameEngine(val playerA: Player, val playerB: Player) {

    val players = listOf(playerA, playerB)

    fun start(activity: GameActivity) {
        var over = false
        var aPlays = false

        for (player in players) {
            if (player.showShips) {
                player.view.showShips(player.model.ships)
            }
        }

        while (!over) {
            aPlays = !aPlays // Player A starts
            over = if (aPlays) {
                turn(playerA, playerB)
            } else {
                turn(playerB, playerA)
            }
        }

        activity.runOnUiThread {
            activity.onGameEnd(aPlays)
        }
    }

    private fun turn(player: Player, opponent: Player): Boolean {
        var prevResult = ShootResult.MISS
        do {
            var position: Coordinate

            var first = true
            do {
                if (!first) prevResult = ShootResult.MISS
                position = player.listener.await(prevResult)
                first = false
            } while (opponent.model[position])

            val result = opponent.model.shoot(position)

            opponent.view.updateTile(position, if (result.second == ShootResult.MISS) TileState.MISS else TileState.HIT)

            if (result.second == ShootResult.SINK) {
                opponent.view.unveilShip(result.first!!)
                if (opponent.model.over) return true
            }

            prevResult = result.second
        } while (result.second != ShootResult.MISS)

        return false
    }

}