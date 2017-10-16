package hu.titi.battleship.net

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import hu.titi.battleship.model.*
import hu.titi.battleship.ui.GameView
import hu.titi.battleship.ui.TileState
import org.jetbrains.anko.runOnUiThread
import kotlin.concurrent.thread

private const val TAG = "client"

class GameClient(private val context: Context,
                 private val disconnectCallback: () -> Unit,
                 private val gameOverCallback: (Boolean) -> Unit) : ServiceConnection {

    private val serviceStore = Store<NetClientService>()
    private val hostViewStore = Store<GameView>()
    private val clientViewStore = Store<GameView>()
    private val listenerStore = Store<PlayerListener>()

    private var exiting = false

    fun setHostView(view: GameView) {
        hostViewStore.set(view)
    }

    fun setClientView(view: GameView) {
        clientViewStore.set(view)
    }

    fun setListener(listener: PlayerListener) {
        listenerStore.set(listener)
    }

    fun setDisconnectListener(listener: () -> Unit) {
        serviceStore.visit().setDisconnectListener(listener)
    }

    fun run() {

        exiting = false
        var running = true
        var won: Boolean? = null

        while (running && won == null && !exiting) {
            val message = serviceStore.visit().awaitMessage()

            Log.i(TAG, "interpreting message: $message")

            val split = message.split(" ")
            when (split[0]) {
                "exit" -> {
                    listenerStore.visitIfPresent()?.abort()
                    running = false
                }

                "show" -> {
                    parseShip(split[1])?.let { ship ->
                        clientViewStore {
                            Log.i(TAG, "[${Thread.currentThread().id}] showing ship - $ship")
                            showShips(listOf(ship))
                        }
                    }
                }

                "unveil" -> {
                    val store = if (split[1] == "a") hostViewStore else clientViewStore
                    parseShip(split[2])?.let { ship ->
                        context.runOnUiThread {
                            store {
                                unveilShip(ship)
                            }
                        }
                    }
                }

                "update" -> {
                    val store = if (split[1] == "a") hostViewStore else clientViewStore
                    val id = split[3].toInt()
                    val state = if (id in 0 until TileState.values().size) TileState.values()[id] else null
                    if (state != null) {
                        parseSafe(split[2])?.let { coordinate ->
                            context.runOnUiThread {
                                store {
                                    updateTile(coordinate, state)
                                }
                            }
                        }
                    }
                }

                "won" -> {
                    won = split[1] == "true"
                }

                "shoot" -> {
                    val click = listenerStore.visit().await(ShootResult.values()[split[1].toInt()])
                    if (click == null) {
                        running = false
                        closeConnection()
                    } else if (!exiting && running) {
                        serviceStore.visit().sendMessage(click.toString())
                    }
                }
            }
        }

        if (!exiting) {
            closeConnection()
            if (won != null) {
                context.runOnUiThread {
                    gameOverCallback(won ?: false)
                }
            } else if (!running) {
                context.runOnUiThread {
                    disconnectCallback()
                }
            }
        }
    }

    fun tryConnect(hostAddress: String) = serviceStore {
        Log.i(TAG, "trying to connect")
        tryConnect(hostAddress)
    }

    fun disconnect() {
        Log.i(TAG, "Aborting")
        listenerStore.visitIfPresent()?.abort()
        exiting = true
    }

    fun closeConnection() {
        serviceStore.visitIfPresent()?.closeConnection()
    }

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        if (binder is NetClientService.NetClientBinder) {
            serviceStore.place(binder.getService())
            Log.i(TAG, "Service connected")
        }
    }

    override fun onServiceDisconnected(className: ComponentName?) {
        Log.i(TAG, "Service disconnected")
        serviceStore.remove()
    }

}