package hu.titi.battleship.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import hu.titi.battleship.R
import hu.titi.battleship.model.*
import hu.titi.battleship.ui.GamePanel
import hu.titi.battleship.ui.ShipView
import org.jetbrains.anko.sdk23.coroutines.onClick
import java.io.Serializable
import kotlin.concurrent.thread

const val MAP_SETUP_OK = 1

class MapSetupActivity : AppCompatActivity() {

    lateinit var gameView: GamePanel
    lateinit var shipView: ShipView
    lateinit var rotated: CheckBox
    lateinit var randomize: Button
    lateinit var undoButton: Button
    lateinit var okButton: Button

    private val ships = mutableListOf<Ship>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        gameView = findViewById(R.id.gameView) as GamePanel
        shipView = findViewById(R.id.shipView) as ShipView
        undoButton = findViewById(R.id.undo) as Button
        randomize = findViewById(R.id.randomize) as Button
        rotated = findViewById(R.id.rotate) as CheckBox
        okButton = findViewById(R.id.ok) as Button

        undoButton.onClick {
            if (ships.isNotEmpty()) {
                ships.removeAt(ships.size - 1)
                shipView.undo()

                gameView.clear()
                gameView.showShips(true, ships)
                for (ship in ships) {
                    gameView.unveilShip(ship)
                }
            }
            okButton.isEnabled = false
        }
        randomize.onClick { randomize() }

        okButton.onClick {
            setResult(MAP_SETUP_OK, Intent().putExtra("map", Map(ships) as Serializable))
            finish()
        }

        if (savedInstanceState != null) {
            val saved = savedInstanceState.getSerializable("ships")
            if (saved != null && saved is List<*>) {
                val savedShips: List<Ship> = (saved as List<*>).filterIsInstance<Ship>()
                ships.clear()
                ships.addAll(savedShips)

                gameView.clear()
                gameView.showShips(true, ships)
                for (ship in ships) {
                    gameView.unveilShip(ship)
                }

                okButton.isEnabled = ships.size == shipSizes.size

                shipView.fromSaved(savedInstanceState)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        thread {
            while (true) {
                val coordinate = gameView.await(ShootResult.MISS) ?: break
                val selected = shipView.getSelected() ?: continue
                val vertical = rotated.isChecked

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
                if (shipView.removeSelected()) {
                    runOnUiThread {
                        okButton.isEnabled = true
                    }
                }
                gameView.showShips(true, listOf(newShip))
                gameView.unveilShip(newShip)
            }
        }
    }

    private fun randomize() {
        val list = randomSetup()

        ships.clear()
        ships.addAll(list)
        shipView.allUsed()

        gameView.clear()
        gameView.showShips(true, ships)
        for (ship in ships) {
            gameView.unveilShip(ship)
        }
        okButton.isEnabled = true

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("ships", ships as Serializable)
        shipView.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        gameView.abort()
        super.onStop()
    }
}
