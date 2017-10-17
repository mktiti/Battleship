package hu.titi.battleship.model

import android.os.Bundle
import android.util.Log
import hu.titi.battleship.activity.LocalGameActivity
import hu.titi.battleship.ui.TileState
import java.io.Serializable

enum class GameType : Serializable { PVP, BOT, REMOTE }

private const val TAG = "engine"

class GameEngine(private val playerA: Player,
                 private val playerB: Player,
                 private val gameType: GameType,
                 private var aPlays: Boolean = false,
                 private val messsageUpdate: (Boolean) -> Unit) {

    @Volatile private var running = true

    fun start(activity: LocalGameActivity) {
        var over = false

        if (gameType == GameType.REMOTE) {
            playerA.view.showShips(false, playerA.model.ships)
            playerB.view.showShips(false, playerB.model.ships)
        } else if (gameType == GameType.BOT) {
            playerA.view.showShips(false, playerA.model.ships)
        }

        Log.i(TAG, "engine start")

        listOf(playerA, playerB).forEach { p ->
            for (x in 0 until SIZE) {
                for (y in 0 until SIZE) {
                    if (p.model[x, y]) {
                        val coordinate = Coordinate.of(x, y)
                        Log.i(TAG, "updating: ($x, $y)")
                        p.view.updateTile(coordinate, if (p.model.shoot(coordinate).second == ShootResult.MISS) TileState.MISS else TileState.HIT)
                    }
                }
            }
        }

        while (running && !over) {
            aPlays = !aPlays // Player A starts
            messsageUpdate(aPlays)
            val overError = if (aPlays) {
                turn(playerA, playerB)
            } else {
                turn(playerB, playerA)
            }

            over = if (overError == null) {
                activity.runOnUiThread {
                    activity.onDisconnected()
                }
                destroy()
                true
            } else {
                overError
            }
        }

        if (running) {
            playerA.view.showShips(true, playerA.model.ships)
            playerB.view.showShips(true, playerB.model.ships)

            playerA.view.gameOver(aPlays)
            playerB.view.gameOver(!aPlays)

            activity.runOnUiThread {
                activity.onGameEnd(aPlays)
            }
        }
    }

    private fun turn(player: Player, opponent: Player): Boolean? {
        var prevResult = ShootResult.MISS
        do {
            var position: Coordinate

            var first = true
            do {
                if (!first) prevResult = ShootResult.MISS
                position = player.listener.await(prevResult) ?: return null
                if (!running) return true

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

    fun save(outState: Bundle) {
        outState.putSerializable("mapA", playerA.model)
        outState.putSerializable("mapB", playerB.model)
        outState.putBoolean("aPlays", !aPlays)
    }

    fun destroy() {
        Log.i(TAG, "Aborting (destroy)")
        running = false
        playerA.listener.abort()
        playerB.listener.abort()
    }

}