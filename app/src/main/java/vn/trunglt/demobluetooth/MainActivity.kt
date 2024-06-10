package vn.trunglt.demobluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.trunglt.demobluetooth.adapters.DeviceAdapter
import vn.trunglt.demobluetooth.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val foundedDeviceAdapter by lazy {
        DeviceAdapter {
            connect(it)
        }
    }
    private val bluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    if (deviceName != null) {
                        val list = foundedDeviceAdapter.currentList.toMutableList()
                        list.add(device)
                        foundedDeviceAdapter.submitList(list)
                        println("${deviceName}=${deviceHardwareAddress}")
                    }
//                    if (deviceName == "TRUNGLT") {
//                        bluetoothDevice = device
//                        bluetoothAdapter.cancelDiscovery()
//                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE
                    )
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            startConnectSocket()
                        }

                        BluetoothDevice.BOND_BONDING -> {

                        }

                        BluetoothDevice.BOND_NONE -> {

                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    connect(bluetoothDevice)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupAdapter()
        registerDiscoveryBluetoothReceiver()
        queryPairedDevicesUsingBluetooth()
        setListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun queryPairedDevicesUsingBluetooth() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val requestCode = 1
            requestPermissions(getBluetoothPermissions(), requestCode)
        } else {
//            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//            pairedDevices?.forEach { device ->
//                val deviceName = device.name
//                val deviceHardwareAddress = device.address // MAC address
//                println("${deviceName}===${deviceHardwareAddress}")
//                if (deviceName == "TRUNGLT") {
//                    bluetoothDevice = device
//                    startConnectSocket()
//                    println("Kết nối socket thành công")
//                }
//            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            queryPairedDevicesUsingBluetooth()
            return
        }
        if (requestCode == 2 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startDiscoverBluetooth()
            return
        }
        if (requestCode == 3 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            listenForBluetoothConnections()
            return
        }
        if (requestCode == 4 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            return
        }
    }

    private fun getBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH
            )
        }
    }

    private fun setListener() {
        findViewById<TextView>(R.id.tv_hello_world).setOnClickListener {
            startDiscoverBluetooth()
        }
        findViewById<TextView>(R.id.tv_enable_socket_server).setOnClickListener {
            listenForBluetoothConnections()
        }
        findViewById<TextView>(R.id.tv_send_data).setOnClickListener {
            sendDataUsingBluetoothSocket()
        }
    }

    private fun startDiscoverBluetooth(): Boolean {
        checkLocationPermission()
        return bluetoothAdapter.startDiscovery()
    }

    private fun registerDiscoveryBluetoothReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocation()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            2
        )
    }

    private fun checkBackgroundLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                2
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                2
            )
        }
    }

    private fun listenForBluetoothConnections() {
        val requestCode = 3
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), requestCode)
            } else {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH), requestCode)
            }
        } else {
            try {
                val bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    application.packageName,
                    UUID.fromString("TRUNGLETHANHVNPAY")
                )
                Thread {
                    val bluetoothSocket = bluetoothServerSocket.accept()
                    val message = String(bluetoothSocket.inputStream.readBytes())
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun connect(device: BluetoothDevice?) {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(getBluetoothPermissions(), 4)
        } else {
            showToast(device?.createBond() == true)
        }
    }

    private fun showToast(isSuccess: Boolean) {
        if (isSuccess) {
            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startConnectSocket() {
        lifecycleScope.launch(Dispatchers.IO) {
            bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(
                bluetoothDevice?.uuids?.lastOrNull()?.uuid
            )
            bluetoothSocket?.connect()
        }
    }

    private fun sendDataUsingBluetoothSocket() {
        println("Trạng thái kết nối: ${bluetoothSocket?.isConnected}")
        if (bluetoothSocket?.isConnected == true) {
            lifecycleScope.launch(Dispatchers.IO) {
                bluetoothSocket?.outputStream?.write("HEHE".toByteArray())
                println("Đã gửi dữ liệu")
            }
        }
    }

    private fun setupAdapter() {
        binding.rvFoundedDevices.adapter = foundedDeviceAdapter
    }
}