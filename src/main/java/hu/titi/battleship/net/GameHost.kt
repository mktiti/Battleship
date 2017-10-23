package hu.titi.battleship.net

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import hu.titi.battleship.model.*
import hu.titi.battleship.ui.TileState

private const val TAG = "host"

class GameHost : ServiceConnection, PlayerListener {

    private val serviceStore = Store<NetHostService>()

    override fun await(prevResult: ShootResult) = serviceStore.visit().await(prevResult)

    fun awaitSetup(): List<Ship>? = serviceStore.visit().awaitSetup()

    override fun abort() = serviceStore.remove()

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        if (binder is NetHostService.NetHostBinder) {
            serviceStore.place(binder.getService())
            Log.i(TAG, "Service connected")
        }
    }

    fun startAndWait() = serviceStore.visit().startAndWait()

    private fun sendMessage(message: String): Boolean = serviceStore {
        sendMessage(message)
    }

    fun closeConnection() {
        serviceStore.visitIfPresent()?.closeConnection()
    }

    fun setDisconnectListener(listener: () -> Unit) =
            serviceStore.visit().setDisconnectListener(listener)

    fun gameOver(won: Boolean) {
        serviceStore {
            sendMessage("won $won")
        }
    }

    fun updateTile(isHost: Boolean, position: Coordinate, state: TileState) = sendMessage("update ${isHost.aOrB()} $position ${state.ordinal}")

    fun unveilShip(isHost: Boolean, ship: Ship) = sendMessage("unveil ${isHost.aOrB()} $ship")

    fun showShips(isHost: Boolean, ships: Collection<Ship>) = ships.forEach { ship ->
        sendMessage("show $isHost $ship")
    }

    override fun onServiceDisconnected(className: ComponentName?) {
        Log.i(TAG, "Service disconnected")
        serviceStore.remove()
    }
}

private fun Boolean.aOrB() = if (this) "a" else "b"