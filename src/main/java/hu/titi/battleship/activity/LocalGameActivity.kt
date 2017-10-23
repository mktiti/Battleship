package hu.titi.battleship.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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

enum class GameState { A_SETUP, B_SETUP, START, WAITING_FOR_SETUP, RUNNING, FINISHED }

private const val A_SETUP_REQUEST = 1
private const val B_SETUP_REQUEST = 2

class LocalGameActivity : AppCompatActivity() {

    private var gameEngine: GameEngine? = null
    private lateinit var type: GameType
    private lateinit var host: GameHost

    private lateinit var messageView: TextView
    private lateinit var playerAPanel: GamePanel
    private lateinit var playerBPanel: GamePanel

    private var state = GameState.A_SETUP

    private var mapA: Map? = null
    private var mapB: Map? = null
    private var aPlays: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_game)

        type = intent.getSerializableExtra("type") as GameType

        if (type == GameType.REMOTE) {
            host = GameHost()
            bindService(Intent(this@LocalGameActivity, NetHostService::class.java), host, Context.BIND_AUTO_CREATE)
        }

        messageView = findViewById(R.id.messageText) as TextView
        playerAPanel = findViewById(R.id.leftPanel) as GamePanel
        playerBPanel = findViewById(R.id.rightPanel) as GamePanel

        mapA = savedInstanceState?.getSerializable("mapA") as? Map?
        mapB = savedInstanceState?.getSerializable("mapB") as? Map?
        aPlays = savedInstanceState?.getBoolean("aPlays") == true

        state = savedInstanceState?.getSerializable("state") as? GameState ?: GameState.A_SETUP

        if (type == GameType.BOT) {
            mapB = Map()
        } else if (type == GameType.REMOTE) {
            thread {
                val list = host.awaitSetup()
                if (list == null) {
                    runOnUiThread {
                        onDisconnected()
                    }
                } else {
                    mapB = Map(list)
                    if (state == GameState.WAITING_FOR_SETUP) {
                        state = GameState.START
                        runOnUiThread {
                            startGame()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val map = data?.getSerializableExtra("map") as? Map
        if (resultCode == MAP_SETUP_OK && map != null) {
            when (requestCode) {
                A_SETUP_REQUEST -> {
                    mapA = map
                    state = when (type) {
                        GameType.PVP -> GameState.B_SETUP
                        GameType.BOT -> GameState.START
                        GameType.REMOTE -> if (mapB == null) {
                            GameState.WAITING_FOR_SETUP
                        } else {
                            GameState.START
                        }
                    }
                }
                B_SETUP_REQUEST -> {
                    mapB = map
                    state = GameState.START
                }
            }
        }
    }

    private fun startGame() {
        val (a: Player, b: Player) = when (type) {
            GameType.PVP -> {
                Pair(Player(mapA!!, view = playerAPanel, listener = playerBPanel),
                        Player(mapB!!, view = playerBPanel, listener = playerAPanel))
            }
            GameType.BOT -> {
                val playerA = Player(mapA!!, view = playerAPanel, listener = playerBPanel)
                Pair(playerA, Player(mapB!!, view = playerBPanel, listener = Bot(playerA.model)))
            }
            GameType.REMOTE -> {
                Pair(Player(mapA!!, view = Remote(true, host, playerAPanel), listener = playerBPanel),
                        Player(mapB!!, view = Remote(false, host, playerBPanel), listener = host))
            }
        }

        gameEngine = GameEngine(a, b, type, aPlays, this::updateMessage)

        thread(name = "GameEngine") {
            if (type == GameType.REMOTE) {
                host.setDisconnectListener { gameEngine?.destroy() }
            }
            gameEngine?.start(this@LocalGameActivity)
        }
        state = GameState.RUNNING
    }

    override fun onStart() {
        super.onStart()

        when (state) {
            GameState.A_SETUP -> startActivityForResult(intentFor<MapSetupActivity>(), A_SETUP_REQUEST)
            GameState.B_SETUP -> startActivityForResult(intentFor<MapSetupActivity>(), B_SETUP_REQUEST)
            GameState.START -> startGame()
            GameState.WAITING_FOR_SETUP -> {
                messageView.textResource = R.string.waiting_for_setup
            }
            else -> { }
        }
    }

    private fun updateMessage(isAPlaying: Boolean) = runOnUiThread {
        messageView.textResource = when (type) {
            GameType.PVP -> if (isAPlaying) R.string.player_a_turn else R.string.player_b_turn
            else -> if (isAPlaying) R.string.your_turn else R.string.opponent_turn
        }
    }

    fun onGameEnd(aWon: Boolean) {
        messageView.textResource = when {
            type == GameType.PVP -> if (aWon) R.string.player_a_wins else R.string.player_b_wins
            aWon -> R.string.you_won
            else -> R.string.opponent_won
        }
        state = GameState.FINISHED
    }

    fun onDisconnected() {
        alert(R.string.player_b_disconnect, R.string.error) {
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
        gameEngine?.save(outState)
        outState.putSerializable("state", state)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (mapA == null || mapB == null || state == GameState.FINISHED) {
            finish()
            return
        }

        alert(R.string.confirm_exit) {
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
