package com.example.bluetoothapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothapp.ui.theme.BluetoothAppTheme

class MainActivity : ComponentActivity() {
    private val PERMISSION_CODE = 1
    private val bluetoothAdapter : BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        result ->
        if (result.resultCode == RESULT_OK){
            Log.i("Bluetooth", ":request permission result ok")
        }
        else{
            Log.i("Bluetooth", ":request permission result canceled / denied")
        }
    }

    private fun requestBluetoothPermission() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activityResultLauncher.launch(enableBluetoothIntent)
    }

    @SuppressLint("MissingPermission")
    public val pairedDevices = bluetoothAdapter.bondedDevices

    var discoveredDevices : Set<BluetoothDevice> = emptySet()
    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                BluetoothDevice.ACTION_FOUND -> {
                    val device : BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null){
                        val updated = discoveredDevices.plus(device)
                        discoveredDevices = updated
                    }
                    Log.i("Bluetooth", "onReceive: Device found")
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i("Bluetooth", "onReceive: Started Discovery")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("Bluetooth", "onReceive: Finished Discovery")
                }
            }
        }
    }

    @SuppressLint("SupportAnnotationUsage")
    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    fun scan() : Set<BluetoothDevice>{

        if (bluetoothAdapter.isDiscovering){
            bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
        }
        else{
            bluetoothAdapter.startDiscovery()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothAdapter.cancelDiscovery()
        }, 10000L)
        return discoveredDevices
    }
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val startFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        val endFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(receiver, foundFilter)
        registerReceiver(receiver, startFilter)
        registerReceiver(receiver, endFilter)

        if(!bluetoothAdapter.isEnabled){
            requestBluetoothPermission()
        }

        if(SDK_INT >= Build.VERSION_CODES.O){
            if(ContextCompat.checkSelfPermission(
                baseContext, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_CODE
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            var devices : Set<BluetoothDevice> by remember { mutableStateOf(emptySet()) }
            BluetoothAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Scaffold (
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Bluetooth Connected List",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center)
                                }
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(top = 20.dp),
                            horizontalAlignment =  Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(100.dp))
                            Button(
                                onClick = {devices = scan()}
                            ) {
                                Text(
                                    "Scan",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Scanned Devices",
                                style = MaterialTheme.typography.titleSmall
                            )
                            discoveredDevices.forEach { de ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 10.dp, 5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = de.name)
                                        Text(text = de.address)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Paired Devices",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            pairedDevices.forEach { device ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 10.dp, 5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = device.name)
                                        Text(text = device.address)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if(bluetoothAdapter.isDiscovering)
            bluetoothAdapter.cancelDiscovery()

        unregisterReceiver(receiver)
    }
}

@Composable
fun Bluetooth(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
