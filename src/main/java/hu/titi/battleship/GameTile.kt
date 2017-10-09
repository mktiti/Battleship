package hu.titi.battleship

import android.content.Context
import android.graphics.Color
import android.view.View
import org.jetbrains.anko.backgroundColor

enum class TileState(val color: Int) {
    UNKNOWN(Color.WHITE),
    SHIP(Color.GRAY),
    MISS(Color.BLUE),
    HIT(Color.RED)
}

class GameTile(context: Context, val coordinate: Coordinate, state: TileState = TileState.UNKNOWN) : View(context) {

    var state = state
        set(value) {
            if (field != value) {
                field = value
                backgroundColor = state.color
            }
        }

    init {
        this.state = state
    }

}