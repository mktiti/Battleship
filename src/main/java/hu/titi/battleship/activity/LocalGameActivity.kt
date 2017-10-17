package hu.titi.battleship.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import hu.titi.battleship.R
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
    private lateinit var type: GameType
    private lateinit var host: GameHost

    private lateinit var messageView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_game)

        type = intent.getSerializableExtra("type") as GameType

        if (type == GameType.REMOTE) {
            host = GameHost()
            bindService(Intent(this@LocalGameActivity, NetHostService::class.java), host, Context.BIND_AUTO_CREATE)
        }

        val playerAPanel = findViewById(R.id.leftPanel) as GamePanel
        val playerBPanel = findViewById(R.id.rightPanel) as GamePanel
        messageView = findViewById(R.id.messageText) as TextView

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
            GameType.REMOTE -> {
                Pair(Player(mapA, view = Remote(true, host, playerAPanel), listener = playerBPanel),
                        Player(mapB, view = Remote(false, host, playerBPanel), listener = host))
            }
        }

        gameEngine = GameEngine(a, b, type, aPlays, this::updateMessage)

        /*
        if (type == GameType.REMOTE) {
            thread {
                host.setDisconnectListener(gameEngine::destroy)
            }
        }
        */

        thread(name = "GameEngine") {
            if (type == GameType.REMOTE) {
                host.setDisconnectListener(gameEngine::destroy)
            }
            gameEngine.start(this@LocalGameActivity)
        }
    }

    fun updateMessage(isAPlaying: Boolean) {
        runOnUiThread {
            messageView.textResource = when (type) {
                GameType.PVP -> if (isAPlaying) R.string.player_a_turn else R.string.player_b_turn
                else -> if (isAPlaying) R.string.your_turn else R.string.opponent_turn
            }
        }
    }

    fun onGameEnd(aWon: Boolean) {
        messageView.textResource = when {
            type == GameType.PVP -> if (aWon) R.string.player_a_wins else R.string.player_b_wins
            aWon -> R.string.you_won
            else -> R.string.opponent_won
        }
        /*
        alert(message, "Game over") {
            okButton {
                this@LocalGameActivity.finish()
            }
            onCancelled {
                this@LocalGameActivity.finish()
            }
        }.show()
        */
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
        if (type == GameType.REMOTE) {
            unbindService(host)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        gameEngine.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        alert("Are you sure you want to quit?") {
            yesButton {
                if (type == GameType.REMOTE) {
                    doAsync {
                        host.closeConnection()
                        runOnUiThread {
                            this@LocalGameActivity.finish()
                        }
                    }
                } else {
                    this@LocalGameActivity.finish()
                }
            }
            noButton {  }
        }.show()
    }

}
