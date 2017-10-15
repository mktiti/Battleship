package hu.titi.battleship.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.EditText
import hu.titi.battleship.R
import hu.titi.battleship.net.GameClient
import hu.titi.battleship.net.NetClientService
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk23.listeners.onClick

private const val TAG = "client-activity"

class ConnectActivity : AppCompatActivity() {

    private lateinit var ipEdit: EditText
    private lateinit var client: GameClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = GameClient(ctx, this::onDisconnect, {})
        bindService(intentFor<NetClientService>(), client, Context.BIND_AUTO_CREATE)

        relativeLayout {
            padding = dip(20)
            linearLayout {
                textView {
                    textResource = R.string.ip
                }.lparams(width = wrapContent, height = wrapContent)
                ipEdit = editText {
                    width = dip(500)
                    text = SpannableStringBuilder("192.168.0.129")
                }.lparams(width = matchParent, height = wrapContent)
            }.lparams(width = matchParent, height = wrapContent)

            button {
                textResource = R.string.connect
                onClick {
                    doAsync {
                        client.closeConnection()
                        Log.i(TAG, "Connecting...")
                        val hostAddress = ipEdit.text.toString()
                        if (hostAddress != "" && client.tryConnect(hostAddress)) {
                            runOnUiThread {
                                startActivity<RemoteGameActivity>()
                            }
                        } else {
                            Log.i(TAG, "Could not connect")
                        }
                    }
                }
            }.lparams {
                alignParentBottom()
                width = matchParent
            }
        }

    }

    private fun onDisconnect() {}

    override fun onDestroy() {
        doAsync {
            client.closeConnection()
        }
        unbindService(client)
        super.onDestroy()
    }
}
