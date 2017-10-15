package hu.titi.battleship.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import hu.titi.battleship.net.GameHost
import hu.titi.battleship.model.GameType
import hu.titi.battleship.net.NetHostService
import hu.titi.battleship.R
import org.jetbrains.anko.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

private const val TAG = "host-activity"

class HostActivity : AppCompatActivity() {

    private lateinit var ipShow: TextView
    private lateinit var host: GameHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = GameHost()
        bindService(Intent(this@HostActivity, NetHostService::class.java), host, Context.BIND_AUTO_CREATE)

        verticalLayout {
            padding = 20

            textView {
                width = matchParent
                textResource = R.string.waiting_for_connection
            }

            ipShow = textView {
                textResource = R.string.waiting_for_ip
                width = matchParent
            }
        }


        doAsync {
            val ips = getIPAddress()
            uiThread {
                for (ip in ips) {
                    ipShow.append("\n${ip.hostAddress}")
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()

        Log.i(TAG, "start")
        thread(name = "Host Close-Await") {
            host.closeConnection()
            if (host.startAndWait()) {
                runOnUiThread {
                    startActivity<LocalGameActivity>("type" to GameType.REMOTE_HOST)
                }
            }
        }
    }

    override fun onStop() {
        /*thread(name = "Host Close") {
            host.closeConnection()
        }
        */
        super.onStop()
    }

    private fun getIPAddress(): List<InetAddress> {
        return NetworkInterface.getNetworkInterfaces().asSequence().flatMap {
            it.inetAddresses.asSequence().filter { address ->
                !address.isLoopbackAddress && address is Inet4Address
            }
        }.toList()
    }

    override fun onDestroy() {
        doAsync {
            host.closeConnection()
        }
        unbindService(host)
        super.onDestroy()
    }
}
