package hu.titi.battleship.net

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

private const val TAG = "client-service"

class NetClientService : Service() {

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    private val store = LinkedBlockingQueue<String>()
    @Volatile private var exiting = false
    @Volatile private var onDisconnect: (() -> Unit)? = null

    inner class NetClientBinder : Binder() {
        fun getService() = this@NetClientService
    }

    override fun onBind(intent: Intent?): IBinder = NetClientBinder()

    private fun listen(clientSocket: Socket) {
        if (clientSocket.isClosed) {
            Log.i(TAG, "Socket closed, not reading")
            return
        }

        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        Log.i(TAG, "Starting to listen")

        exiting = false
        try {
            var line: String?
            while (!exiting) {
                line = reader.readLine()

                if (line == null || line == "exit") {
                    Log.i(TAG, "Socket closed or exit signal received, exiting")
                    exiting = true
                    store.clear()
                    store.put("exit")
                    onDisconnect?.invoke()
                } else {
                    Log.i(TAG, "received message: $line")
                    store.add(line)
                }
            }
        } catch (se: SocketException) {
            Log.i(TAG, "Socket closed")
        }

    }

    fun tryConnect(hostAddress: String): Boolean {
        try {
            store.clear()
            Log.i(TAG, "trying to connect to $hostAddress")
            socket = Socket(hostAddress, PORT)
            socket?.let {
                if (!it.isClosed) {
                    writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                    thread(name = "NetClient Listener") {
                        listen(it)
                    }
                }
            }
            if (socket?.isConnected != false) return true
        } catch (se: SocketException) {
            Log.i(TAG, "SocketException, socket closed before connection established")
            closeConnection()
        }

        return false
    }

    fun setDisconnectListener(listener: () -> Unit) {
        onDisconnect = listener
    }

    fun awaitMessage(): String = store.take()

    fun sendMessage(message: String): Boolean {
        Log.i(TAG, "sending message: $message")

        return try {
            writer?.apply {
                write(message)
                newLine()
                flush()
            }
            writer != null
        } catch (se: SocketException) {
            Log.i(TAG, "SocketException, socket closed before line written")
            closeConnection()
            false
        }
    }

    fun closeConnection() {
        store.clear()
        store.put("exit")

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
            socket?.close()
        } catch (se: SocketException) {
            Log.i(TAG, "Socket exception while closing (probably not open)")
        }
    }
}