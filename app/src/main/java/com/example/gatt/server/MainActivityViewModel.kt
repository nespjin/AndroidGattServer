package com.example.gatt.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID


private const val SERVICE_UUID = "0000b81d-0000-1000-8000-00805f9b34fb"
private const val WRITEABLE_CHAR_UUID = "7db3e235-3608-41f3-a03c-955fcbd2ea4b"
private const val READABLE_CHAR_UUID = "36d4dc5c-814b-4097-a5a6-b93b39085928"

data class MainActivityUiState(
    val log: String = "",
    val serviceUUID: String = SERVICE_UUID,
    val writeableCharUUID: String = WRITEABLE_CHAR_UUID,
    val readableCharUUID: String = READABLE_CHAR_UUID,
    val bleName: String = "WR-Sample"
)

class MainActivityViewModel(
    private val applicationContext: Context
) : ViewModel() {

    private val bluetoothManager by lazy { applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val advertiser by lazy { bluetoothAdapter.bluetoothLeAdvertiser }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }

    private var gattServer: BluetoothGattServer? = null
    private var isAdverting = false;

    private val _uiState = MutableStateFlow(MainActivityUiState())
    val state: StateFlow<MainActivityUiState> = _uiState

    fun addLog(line: String) {
        _uiState.update {
            val log = it.log + "\n" + line
            it.copy(log = log)
        }
    }

    private fun clearLog() = _uiState.update { it.copy(log = "") }

    fun setBluetoothName(name: String) = _uiState.update { it.copy(bleName = name) }

    fun setServiceUUID(uuid: String) = _uiState.update { it.copy(serviceUUID = uuid) }

    fun setWriteableCharUUID(uuid: String) = _uiState.update { it.copy(writeableCharUUID = uuid) }

    fun setReadableCharUUID(uuid: String) = _uiState.update { it.copy(readableCharUUID = uuid) }

    @SuppressLint("MissingPermission")
    fun startServer() {
        Log.d(TAG, "startServer: ${bluetoothAdapter.address}")
        clearLog()

        val serviceUUID = _uiState.value.serviceUUID
        val writeableCharUUID = _uiState.value.writeableCharUUID
        val readableCharUUID = _uiState.value.readableCharUUID

        val service = BluetoothGattService(
            UUID.fromString(serviceUUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val writeableChar = BluetoothGattCharacteristic(
            UUID.fromString(writeableCharUUID),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val readableChar = BluetoothGattCharacteristic(
            UUID.fromString(readableCharUUID),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(writeableChar)
        service.addCharacteristic(readableChar)

        try {
            this.gattServer?.close()
        } catch (_: Exception) {
        }

        val gattServer = bluetoothManager.openGattServer(applicationContext, callback)
        gattServer.addService(service)
        this.gattServer = gattServer

        val bluetoothName = _uiState.value.bleName
        bluetoothAdapter.name = bluetoothName

        Log.d(TAG, "startServer: BluetoothAdapterName=${bluetoothName}")
        Log.d(TAG, "startServer:  bluetoothAdapter.name=${bluetoothAdapter.name}")
        Log.d(TAG, "startServer: Service UUID=${serviceUUID}")
        Log.d(TAG, "startServer: Writeable CHAR UUID=${writeableCharUUID}")
        Log.d(TAG, "startServer: Readable CHAR UUID=${readableCharUUID}")

        if (isAdverting) {
            advertiser.stopAdvertising(advertiserCallback)
            isAdverting = false
        }
        adverting()
    }

    @SuppressLint("MissingPermission")
    private fun adverting() {
        val settings = AdvertiseSettings.Builder()
            .setConnectable(true)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
            .build()

        advertiser.startAdvertising(settings, data, advertiserCallback)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            addLog("onConnectionStateChange: status=$status, newState=$newState")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            addLog("onCharacteristicWriteRequest: char=${characteristic?.uuid}, value=${value?.toHexString()}")
        }
    }
    private val advertiserCallback = object : AdvertiseCallback() {

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "onStartFailure: Advertising $errorCode")
            addLog("Advertising Failed: $errorCode")
            super.onStartFailure(errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "onStartSuccess: Advertising")
            addLog("Advertising Success")
            isAdverting = true
            super.onStartSuccess(settingsInEffect)
        }
    }

    companion object {
        private const val TAG = "MainActivityViewModel"
    }
}