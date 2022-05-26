package com.example.valvereceiver

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.valvereceiver.Constants.MY_TAG
import com.example.valvereceiver.Constants.SERVICE_UUID
import com.example.valvereceiver.Constants.allCharacteristics
import com.example.valvereceiver.ui.theme.ValveReceiverTheme
import java.util.*


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var gattServer: BluetoothGattServer

    val devices = mutableListOf<BluetoothDevice>()

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(MY_TAG, "Peripheral advertising started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(MY_TAG, "Peripheral advertising failed: $errorCode")
        }
    }

    inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (device != null) {
                    Log.d(MY_TAG, "${device.address} STATE_CONNECTED")
                    devices.add(device)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(MY_TAG, "${device?.address} STATE_DISCONNECTED")
                devices.remove(device)
            }
        }

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            if (allCharacteristics.contains(characteristic?.uuid)) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                if (value != null) {
//                    val length = value.size
//                    val reversed = ByteArray(length)
//                    for (i in 0 until length) {
//                        reversed[i] = value[length - (i + 1)]
//                    }
//                    characteristic?.value = reversed
                    characteristic?.value = value
                    for (dev in devices) {
                        gattServer.notifyCharacteristicChanged(dev, characteristic, false)
                    }
                } else {
                    Log.d(MY_TAG, "onCharacteristicWriteRequest value is null")
                }
            }
        }
    }

    private val permissionsOldApi = listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private val permissionsApi29 = listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionsApi31 = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            map.entries.forEach {
                if (it.value) {
                    Log.d(MY_TAG, "${it.key} permission is granted")
                    Toast.makeText(
                        this,
                        "isGranted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d(MY_TAG, "${it.key} permission is NOT granted")
                    Toast.makeText(
                        this,
                        "not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = when {
            isApi(Build.VERSION_CODES.S) -> permissionsApi31
            isApi(Build.VERSION_CODES.Q) -> permissionsApi29
            else -> permissionsOldApi
        }
        checkPermissions(permissions)
        checkBLE()
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        setContent {
            ValveReceiverTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    override fun onResume() {
        super.onResume()
        val gattServerCallback = GattServerCallback()
        gattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        setupServer()
        startAdvertising()
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    override fun onStop() {
        super.onStop()
        stopAdvertising()
        stopServer()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun stopServer() {
        gattServer.close()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_ADVERTISE")
    private fun stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    private fun checkPermissions(permissions: List<String>) {
        permissions.filter {
            checkPermission(it).not()
        }.also {
            Log.d(MY_TAG, "permissions ask")
            requestPermissionLauncher.launch(it.toTypedArray())
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun setupServer() {
        Log.d(MY_TAG, "setupServer")
        val service = BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        allCharacteristics.map { it.createBaseWriteCharacteristic() }.forEach { characteristic ->
            service.addCharacteristic(characteristic)
        }
        gattServer.addService(service)
//        val writeCharacteristic = BluetoothGattCharacteristic(
//
//        )
//        val clientConfigurationDescriptor = BluetoothGattDescriptor(
//            CLIENT_CONFIGURATION_DESCRIPTOR_UUID,
//            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
//        ).apply {
//            value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
//        }
//        val notifyCharacteristic = BluetoothGattCharacteristic(
//            CHARACTERISTIC_TIME_UUID,
//            0,
//            0
//        )
//        notifyCharacteristic.addDescriptor(clientConfigurationDescriptor)
//        gattServer.addService(
//            BluetoothGattService(
//                SERVICE_UUID,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY
//            ).apply {
//                addCharacteristic(writeCharacteristic)
//                addCharacteristic(notifyCharacteristic)
//            }
//        )
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_ADVERTISE")
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .build()
        val parcelUuid = ParcelUuid(SERVICE_UUID)
        val data = AdvertiseData.Builder()
//            .setIncludeDeviceName(true) // The advertisement will not have enough space for both the device name and a 16 byte service UUID
            .addServiceUuid(parcelUuid)
            .build()
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }


    private fun checkPermission(permissionName: String) = ContextCompat.checkSelfPermission(
        this,
        permissionName
    ) == PackageManager.PERMISSION_GRANTED.also {
        Log.d(MY_TAG, "$permissionName PERMISSION_GRANTED")
    }

    private fun checkBLE() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(MY_TAG, "Support BLUETOOTH_LE")
        } else {
            Log.d(MY_TAG, "NOT Support BLUETOOTH_LE")
        }
    }

    private fun UUID.createBaseWriteCharacteristic() = BluetoothGattCharacteristic(
        this,
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ValveReceiverTheme {
        Greeting("Android")
    }
}

fun isApi(apiNumber: Int) = Build.VERSION.SDK_INT >= apiNumber

