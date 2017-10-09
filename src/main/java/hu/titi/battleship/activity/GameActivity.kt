package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import hu.titi.battleship.*
import hu.titi.battleship.Map
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.linearLayout

class GameActivity : AppCompatActivity() {

    lateinit var gameEngine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerAPanel = GamePanel(this)
        val playerBPanel = GamePanel(this)

        linearLayout {
            weightSum = 2F
            addView(playerAPanel)
            addView(playerBPanel)
        }

        val pvp = intent.getBooleanExtra("pvp", true)

        val playerA = Player(Map(), view = playerAPanel, listener = playerBPanel, showShips = !pvp)
        val playerB = if (pvp) {
            Player(Map(), view = playerBPanel, listener = playerAPanel, showShips = false)
        } else {
            Player(Map(), view = playerBPanel, listener = Bot(playerA.model), showShips = false)
        }

        gameEngine = GameEngine(playerA, playerB)

        doAsync {
            gameEngine.start(this@GameActivity)
        }
    }

    fun onGameEnd(aWon: Boolean) {
        alert("Congrats Player ${if (aWon) "1" else "2"}!", "GameEngine over").show()
    }
}
