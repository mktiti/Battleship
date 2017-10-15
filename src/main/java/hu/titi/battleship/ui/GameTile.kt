package hu.titi.battleship.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import hu.titi.battleship.model.Coordinate
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