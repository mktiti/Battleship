package hu.titi.battleship.net

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import hu.titi.battleship.model.Coordinate
import hu.titi.battleship.model.ShootResult
import hu.titi.battleship.model.Store
import hu.titi.battleship.model.parseSafe
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
    @Volatile private var exiting = false
    @Volatile private var queryEnabled = false

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
                    }
                    line == "exit" -> {
                        Log.i(TAG, "EXIT signal received")
                        exiting = true
                        store.place(null)
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

    fun startAndWait(): Boolean {
        try {
            serverSocket = ServerSocket(PORT)
            Log.i(TAG, "Server socket accepting")
            clientSocket = serverSocket?.accept()
            Log.i(TAG, "Server socket accepting ended")
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