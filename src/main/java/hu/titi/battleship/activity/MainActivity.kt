package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import hu.titi.battleship.R
import org.jetbrains.anko.button
import org.jetbrains.anko.sdk23.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textResource
import org.jetbrains.anko.verticalLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            button {
                textResource = R.string.start_pvp
                onClick {
                    startActivity<GameActivity>("pvp" to true)
                }
            }

            button {
                textResource = R.string.start_pvcpu
                onClick {
                    startActivity<GameActivity>("pvp" to false)
                }
            }

            button {
                textResource = R.string.connect_to_game
                onClick {
                    startActivity<ConnectActivity>()
                }
            }

            button {
                textResource = R.string.host_game
                onClick {
                    startActivity<HostActivity>()
                }
            }

            button {
                textResource = R.string.settings
            }
        }

    }
}