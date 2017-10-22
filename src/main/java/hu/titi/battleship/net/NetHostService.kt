package hu.titi.battleship.net

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import hu.titi.battleship.model.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

const val PORT = 12345

private const val TAG = "host-service"

class NetHostService : Service() {

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: BufferedWriter? = null

    private val store = Store<Coordinate?>()
    private val setupStore = Store<List<Ship>?>()
    @Volatile private var exiting = false
    @Volatile private var queryEnabled = false
    @Volatile private var onDisconnect: (() -> Unit)? = null

    inner class NetHostBinder : Binder() {
        fun getService() = this@NetHostService
    }

    override fun onBind(intent: Intent?): IBinder = NetHostBinder()

    private fun listen(clientSocket: Socket) {
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        exiting = false
        try {
            Log.i(TAG, "listening")
            var line: String?
            while (!exiting) {
                line = reader.readLine()

                when {
                    line == null -> {
                        Log.i(TAG, "Read line return null, exiting")
                        exiting = true
                        store.place(null)
                        setupStore.place(null)
                        onDisconnect?.invoke()
                    }
                    line == "exit" -> {
                        Log.i(TAG, "EXIT signal received")
                        exiting = true
                        store.place(null)
                        setupStore.place(null)
                        onDisconnect?.invoke()
                    }
                    line.startsWith("setup ") -> {
                        val list = line.substring(6).split("|").map(::parseShip)
                        if (list.any { it == null }) {
                            Log.i(TAG, "Setup error")
                            setupStore.place(null)
                            store.place(null)
                            onDisconnect?.invoke()
                        } else {
                            setupStore.place(list.requireNoNulls())
                        }
                    }
                    queryEnabled -> parseSafe(line)?.let {
                        store.place(it)
                    }
                    else -> Log.i(TAG, "Dropping line: $line")
                }
            }
        } catch (se: SocketException) {
            Log.i(TAG, "Client socket closed (listening)")
        }

    }

    fun setDisconnectListener(listener: () -> Unit) {
        onDisconnect = listener
    }

    fun startAndWait(): Boolean {
        try {
            serverSocket = ServerSocket(PORT)
            Log.i(TAG, "Server socket accepting")
            clientSocket = serverSocket?.accept()
            Log.i(TAG, "Server socket accepting ended")
            store.remove()
            clientSocket?.let {
                writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                thread(name = "NetHost Listener") {
                    listen(it)
                }
            }
            return clientSocket != null
        } catch (se: SocketException) {
            Log.i(TAG, "SocketException, server socket closed before connection established")
            closeConnection()
        }
        return false
    }

    fun await(prevResult: ShootResult): Coordinate? {
        sendMessage("shoot ${prevResult.ordinal}")
        queryEnabled = true
        val coordinate = store.await()
        queryEnabled = false
        return coordinate
    }

    fun awaitSetup(): List<Ship>? = setupStore.await()

    fun sendMessage(message: String): Boolean {
        Log.i(TAG, "sending message: $message")

        return try {
            writer?.write(message)
            writer?.newLine()
            writer?.flush()
            writer != null
        } catch (se: SocketException) {
            Log.i(TAG, "SocketException, server socket closed before line written")
            closeConnection()
            false
        }
    }

    fun closeConnection() {
        writer?.apply {
            try {
                write("exit")
                newLine()
                flush()
            } catch (se: SocketException) {
                Log.i(TAG, "Socket exception while sending exit sign")
            }
        }

        exiting = true
        writer = null

        try {
            serverSocket?.close()
        } catch (se: SocketException) {
            Log.i(TAG, "Server socket exception while closing (probably not open)")
        }

        try {
            clientSocket?.close()
        } catch (se: SocketException) {
            Log.i(TAG, "Client socket exception while closing (probably not open)")
        }
    }
}