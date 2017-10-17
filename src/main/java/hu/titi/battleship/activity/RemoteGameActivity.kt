package hu.titi.battleship.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import hu.titi.battleship.R
import hu.titi.battleship.net.GameClient
import hu.titi.battleship.net.NetClientService
import hu.titi.battleship.ui.GamePanel
import org.jetbrains.anko.*
import kotlin.concurrent.thread

class RemoteGameActivity : AppCompatActivity() {

    private lateinit var client: GameClient
    private lateinit var messageView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game)
        val clientPanel = findViewById(R.id.leftPanel) as GamePanel
        val hostPanel = findViewById(R.id.rightPanel) as GamePanel
        messageView = findViewById(R.id.messageText) as TextView

        client = GameClient(ctx, this::onDisconnected, this::onGameEnd, this::updateMessage)
        bindService(intentFor<NetClientService>(), client, Context.BIND_AUTO_CREATE)

        client.setHostView(hostPanel)
        client.setClientView(clientPanel)
        client.setListener(hostPanel)

        thread(name = "NetClient") {
            client.setDisconnectListener(hostPanel::abort)
            client.run()
        }
    }

    fun updateMessage(ownTurn: Boolean) {
        runOnUiThread {
            messageView.textResource = if (ownTurn) R.string.your_turn else R.string.opponent_turn
        }
    }

    private fun onGameEnd(won: Boolean) {
        messageView.textResource = if (won) R.string.you_won else R.string.opponent_won
    }

    private fun onDisconnected() {
        alert("Host has disconnected!", "Error") {
            okButton {
                this@RemoteGameActivity.finish()
            }
            onCancelled {
                this@RemoteGameActivity.finish()
            }
        }.show()
    }

    override fun onDestroy() {
        thread(name = "Client close") {
            client.closeConnection()
        }
        unbindService(client)
        super.onDestroy()
    }

    override fun onBackPressed() {
        alert("Are you sure you want to quit?") {
            yesButton {
                thread(name = "Client close") {
                    client.disconnect()
                    runOnUiThread {
                        this@RemoteGameActivity.finish()
                    }
                }
            }
            noButton {  }
        }.show()
    }

}