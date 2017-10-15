package hu.titi.battleship.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import hu.titi.battleship.net.GameClient
import hu.titi.battleship.net.NetClientService
import hu.titi.battleship.ui.GamePanel
import org.jetbrains.anko.*
import kotlin.concurrent.thread

class RemoteGameActivity : AppCompatActivity() {

    private lateinit var client: GameClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = GameClient(ctx, this::onDisconnected, this::onGameEnd)
        bindService(intentFor<NetClientService>(), client, Context.BIND_AUTO_CREATE)

        val hostPanel = GamePanel(this)
        val clientPanel = GamePanel(this)

        linearLayout {
            weightSum = 2F
            addView(clientPanel)
            addView(hostPanel)
        }

        client.setHostView(hostPanel)
        client.setClientView(clientPanel)
        client.setListener(hostPanel)

        thread(name = "NetClient") {
            client.run()
        }
    }

    private fun onGameEnd(won: Boolean) {
        val message = if (won) "Congratulations, you win!" else "Better luck next time!"
        alert(message, "Game over") {
            okButton {
                this@RemoteGameActivity.finish()
            }
            onCancelled {
                this@RemoteGameActivity.finish()
            }
        }.show()
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