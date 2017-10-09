package hu.titi.battleship.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import hu.titi.battleship.R
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk23.listeners.onClick

class ConnectActivity : AppCompatActivity() {

    lateinit var ipEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        relativeLayout {
            padding = dip(20)
            linearLayout {
                textView {
                    textResource = R.string.ip
                }
                ipEdit = editText {
                    width = dip(500)
                }
            }.lparams(width = matchParent, height = wrapContent)

            button {
                textResource = R.string.connect
                onClick {
                    toast("Connecting to ${ipEdit.text}")
                }
            }.lparams {
                alignParentBottom()
                width = dip(500)
            }
        }

    }
}
