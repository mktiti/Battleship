package hu.titi.battleship.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import hu.titi.battleship.R
import hu.titi.battleship.model.GameType
import hu.titi.battleship.net.GameHost
import hu.titi.battleship.net.NetHostService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textResource
import org.jetbrains.anko.uiThread
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
        setContentView(R.layout.activity_host)

        host = GameHost()
        bindService(Intent(this@HostActivity, NetHostService::class.java), host, Context.BIND_AUTO_CREATE)

        ipShow = findViewById(R.id.ip_view) as TextView
        ipShow.textResource = R.string.waiting_for_ip

        doAsync {
            val ips = getIPAddress()
            uiThread {
                if (ips.isNotEmpty()) {
                    ipShow.text = getString(R.string.ip_is, ips[0].hostAddress)
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
                    startActivity<LocalGameActivity>("type" to GameType.REMOTE)
                }
            }
        }
    }

    private fun getIPAddress(): List<InetAddress> =
            NetworkInterface.getNetworkInterfaces().asSequence().flatMap {
                it.inetAddresses.asSequence().filter { address ->
                    !address.isLoopbackAddress && address is Inet4Address
                }
            }.toList()

    override fun onDestroy() {
        doAsync {
            host.closeConnection()
        }
        unbindService(host)
        super.onDestroy()
    }
}
