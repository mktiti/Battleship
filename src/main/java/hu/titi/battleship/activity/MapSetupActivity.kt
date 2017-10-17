package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import hu.titi.battleship.ui.GamePanel
import hu.titi.battleship.ui.GameView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.linearLayout

class MapSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gamePanel = GamePanel(ctx)

        linearLayout {
            addView(gamePanel)



        }

    }
}
