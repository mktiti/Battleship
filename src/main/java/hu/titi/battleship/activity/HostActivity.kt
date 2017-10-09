package hu.titi.battleship.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import hu.titi.battleship.R
import org.jetbrains.anko.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class HostActivity : AppCompatActivity() {

    lateinit var ipShow: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun getIPAddress(): List<InetAddress> {
        return NetworkInterface.getNetworkInterfaces().asSequence().flatMap {
            it.inetAddresses.asSequence().filter { address ->
                !address.isLoopbackAddress && address is Inet4Address
            }
        }.toList()
    }

}
