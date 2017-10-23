package hu.titi.battleship.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import hu.titi.battleship.R
import hu.titi.battleship.model.Map
import hu.titi.battleship.net.GameClient
import hu.titi.battleship.net.NetClientService
import hu.titi.battleship.ui.GamePanel
import org.jetbrains.anko.*
import kotlin.concurrent.thread

private const val SETUP_REQUEST = 1

class RemoteGameActivity : AppCompatActivity() {

    private lateinit var client: GameClient
    private lateinit var messageView: TextView
    private lateinit var hostPanel: GamePanel

    private var setupWait = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game)
        val clientPanel = findViewById(R.id.leftPanel) as GamePanel
        hostPanel = findViewById(R.id.rightPanel) as GamePanel
        messageView = findViewById(R.id.messageText) as TextView

        client = GameClient(ctx, this::onDisconnected, this::onGameEnd, this::updateMessage)
        bindService(intentFor<NetClientService>(), client, Context.BIND_AUTO_CREATE)

        client.setHostView(hostPanel)
        client.setClientView(clientPanel)
        client.setListener(hostPanel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val map = data?.getSerializableExtra("map") as? Map
        if (resultCode == MAP_SETUP_OK && requestCode == SETUP_REQUEST && map != null) {
            thread(name = "NetClient") {
                client.sendSetup(map.ships)
                client.setDisconnectListener(hostPanel::abort)
                client.run()
            }
            setupWait = false
        }
    }

    override fun onStart() {
        super.onStart()

        if (setupWait) {
            startActivityForResult(intentFor<MapSetupActivity>(), SETUP_REQUEST)
            messageView.textResource = R.string.waiting_for_setup
        }
    }

    private fun updateMessage(ownTurn: Boolean) = runOnUiThread {
        messageView.textResource = if (ownTurn) R.string.your_turn else R.string.opponent_turn
    }

    private fun onGameEnd(won: Boolean) {
        messageView.textResource = if (won) R.string.you_won else R.string.opponent_won
    }

    private fun onDisconnected() {
        alert(R.string.host_disconnected, R.string.error) {
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
        alert(R.string.confirm_exit) {
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