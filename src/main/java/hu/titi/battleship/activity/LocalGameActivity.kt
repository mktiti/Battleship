package hu.titi.battleship.activity

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import hu.titi.battleship.model.*
import hu.titi.battleship.model.Map
import hu.titi.battleship.net.GameHost
import hu.titi.battleship.net.NetHostService
import hu.titi.battleship.net.Remote
import hu.titi.battleship.ui.GamePanel
import org.jetbrains.anko.*
import kotlin.concurrent.thread

class LocalGameActivity : AppCompatActivity() {

    private lateinit var gameEngine: GameEngine
    lateinit var type: GameType
    lateinit var host: GameHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = intent.getSerializableExtra("type") as GameType

        if (type == GameType.REMOTE_HOST) {
            host = GameHost()
            bindService(Intent(this@LocalGameActivity, NetHostService::class.java), host, Context.BIND_AUTO_CREATE)
        }

        val playerAPanel = GamePanel(this)
        val playerBPanel = GamePanel(this)

        linearLayout {
            weightSum = 2F
            addView(playerAPanel)
            addView(playerBPanel)
        }

        val mapA: Map = savedInstanceState?.getSerializable("mapA") as? Map ?: Map()
        val mapB: Map = savedInstanceState?.getSerializable("mapB") as? Map ?: Map()
        val aPlays = savedInstanceState?.getBoolean("aPlays") ?: false

        val (a: Player, b: Player) = when (type) {
            GameType.PVP -> {
                Pair(Player(mapA, view = playerAPanel, listener = playerBPanel),
                        Player(mapB, view = playerBPanel, listener = playerAPanel))
            }
            GameType.BOT -> {
                val playerA = Player(mapA, view = playerAPanel, listener = playerBPanel)
                Pair(playerA, Player(mapB, view = playerBPanel, listener = Bot(playerA.model)))
            }
            GameType.REMOTE_HOST -> {
                Pair(Player(mapA, view = Remote(true, host, playerAPanel), listener = playerBPanel),
                        Player(mapB, view = Remote(false, host, playerBPanel), listener = host))
            }
            GameType.REMOTE_CLIENT -> {
                Pair(Player(mapA, view = Remote(true, host, playerAPanel), listener = playerBPanel),
                        Player(mapB, view = Remote(false, host, playerBPanel), listener = host))
            }
        }

        gameEngine = GameEngine(a, b, type, aPlays)

        thread(name = "GameEngine") {
            gameEngine.start(this@LocalGameActivity)
        }
    }

    fun onGameEnd(aWon: Boolean) {
        val message = when {
            type == GameType.PVP -> "Player ${if (aWon) "1" else "2"} wins!"
            aWon -> "You won!"
            else -> "Better luck next time!"
        }
        alert(message, "Game over") {
            okButton {
                this@LocalGameActivity.finish()
            }
            onCancelled {
                this@LocalGameActivity.finish()
            }
        }.show()
    }

    fun onDisconnected() {
        alert("Player 2 disconnected!", "Error") {
            okButton {
                this@LocalGameActivity.finish()
            }
            onCancelled {
                this@LocalGameActivity.finish()
            }
        }.show()
    }

    override fun onDestroy() {
        unbindService(host)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        gameEngine.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        alert("Are you sure you want to quit?") {
            yesButton {
                if (type == GameType.REMOTE_HOST) {
                    doAsync {
                        host.closeConnection()
                        runOnUiThread {
                            this@LocalGameActivity.finish()
                        }
                    }
                }
            }
            noButton {  }
        }.show()
    }

}
