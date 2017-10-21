package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import hu.titi.battleship.R
import hu.titi.battleship.ui.GamePanel
import hu.titi.battleship.ui.GameView
import hu.titi.battleship.ui.ShipView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.linearLayout

class MapSetupActivity : AppCompatActivity() {

    lateinit var gameView: GamePanel
    lateinit var shipView: ShipView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        gameView = findViewById(R.id.gameView) as GamePanel
        shipView = findViewById(R.id.shipView) as ShipView

    }

    fun onShipSelected(size: Int) {

    }

}
