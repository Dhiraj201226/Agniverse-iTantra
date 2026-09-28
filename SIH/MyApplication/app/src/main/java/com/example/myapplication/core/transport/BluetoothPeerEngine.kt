package com.example.myapplication.core.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/**
 * Offline Dual-Transport Engine: Bluetooth RFCOMM Socket Transceiver.
 * Enables direct P2P mesh communication between two Android devices over Bluetooth
 * when Wi-Fi Direct is unavailable or as a redundant dual-link transport.
 */
class BluetoothPeerEngine(private val context: Context) {

    private val serviceUuid: UUID = UUID.fromString("e8a9462e-128a-4d2a-8b89-123456789abc")
    private val serviceName = "iTANTRA_BT_MESH"

    private var serverSocket: BluetoothServerSocket? = null
    private var activeClientSocket: BluetoothSocket? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startBluetoothListener(onFrameReceived: (ByteArray) -> Unit) {
        if (listenJob?.isActive == true) return

        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!btAdapter.isEnabled) return

        listenJob = scope.launch {
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(serviceName, serviceUuid)

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    activeClientSocket = socket

                    scope.launch {
                        try {
                            val dis = DataInputStream(socket.inputStream)
                            while (isActive && socket.isConnected) {
                                val length = dis.readInt()
                                if (length in 1..65536) {
                                    val frame = ByteArray(length)
                                    dis.readFully(frame)
                                    onFrameReceived(frame)
                                }
                            }
                        } catch (e: Exception) {
                            try { socket.close() } catch (ignored: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                // RFCOMM Server Socket closed or permission missing
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendBluetoothFrame(deviceAddress: String, frame: ByteArray): Boolean {
        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return try {
            val device = btAdapter.getRemoteDevice(deviceAddress)
            val socket = device.createRfcommSocketToServiceRecord(serviceUuid)
            socket.connect()

            val dos = DataOutputStream(socket.outputStream)
            dos.writeInt(frame.size)
            dos.write(frame)
            dos.flush()

            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isBluetoothAvailable(): Boolean {
        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return btAdapter.isEnabled
    }

    fun stop() {
        listenJob?.cancel()
        try { serverSocket?.close() } catch (e: Exception) {}
        try { activeClientSocket?.close() } catch (e: Exception) {}
    }
}
