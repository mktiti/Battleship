package hu.titi.battleship.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import hu.titi.battleship.R
import hu.titi.battleship.model.GameType
import org.jetbrains.anko.button
import org.jetbrains.anko.sdk23.coroutines.onClick
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
                    startActivity<LocalGameActivity>("type" to GameType.PVP)
                }
            }

            button {
                textResource = R.string.start_pvcpu
                onClick {
                    startActivity<LocalGameActivity>("type" to GameType.BOT)
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

            /*
            button {
                text = "Setup"
                onClick {
                    startActivity<MapSetupActivity>("maps" to 2)
                }
            }
            */
        }

    }
}