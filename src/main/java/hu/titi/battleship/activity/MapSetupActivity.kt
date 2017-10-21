package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import hu.titi.battleship.R
import hu.titi.battleship.model.Coordinate
import hu.titi.battleship.model.Ship
import hu.titi.battleship.model.ShootResult
import hu.titi.battleship.model.validCoordinate
import hu.titi.battleship.ui.GamePanel
import hu.titi.battleship.ui.GameView
import hu.titi.battleship.ui.ShipView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.linearLayout
import kotlin.concurrent.thread

class MapSetupActivity : AppCompatActivity() {

    lateinit var gameView: GamePanel
    lateinit var shipView: ShipView
    lateinit var rotateButton: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        gameView = findViewById(R.id.gameView) as GamePanel
        shipView = findViewById(R.id.shipView) as ShipView
        rotateButton = findViewById(R.id.rotate) as CheckBox
    }

    override fun onStart() {
        super.onStart()

        val ships = mutableListOf<Ship>()

        thread {
            while (true) {
                val coordinate = gameView.await(ShootResult.MISS) ?: break
                val selected = shipView.getSelected() ?: continue
                val vertical = rotateButton.isChecked

                val dx = if (vertical) 0 else 1
                val dy = 1 - dx

                val tiles = Array(selected) { i ->
                    Pair(coordinate.x + i * dx, coordinate.y + i * dy)
                }

                if (tiles.any { !validCoordinate(it) }) continue

                val newShip = Ship(selected, coordinate, vertical)

                if (ships.any { ship ->
                    val oldShipSet = mutableSetOf<Coordinate>()
                    oldShipSet.addAll(ship.border())
                    oldShipSet.addAll(ship.tiles.map(Pair<Coordinate, *>::first))

                    Log.i("setup", oldShipSet.toString())
                    Log.i("setup", newShip.tiles.toString())

                    oldShipSet.intersect(newShip.tiles.map(Pair<Coordinate, *>::first)).isNotEmpty()
                }) continue

                ships.add(newShip)
                shipView.removeSelected()
                gameView.showShips(true, listOf(newShip))
            }
        }
    }

    override fun onStop() {
        gameView.abort()
        super.onStop()
    }
}
