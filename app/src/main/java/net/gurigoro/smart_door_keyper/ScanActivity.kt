package net.gurigoro.smart_door_keyper

import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.AdapterView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_scan.*

class ScanActivity : AppCompatActivity() {

    val SDK_SERVICE_UUID = "71495a29-c548-4aa6-a75b-61959767faa2"

    companion object {
        public val DEVICE_ADDRESS_KEY: String = "DeviceAddress"
    }

    lateinit var adapter: DeviceListAdapter
    lateinit var btAdapter: BluetoothAdapter
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let {
                adapter.addDevice(it)
                adapter.notifyDataSetChanged()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        adapter = DeviceListAdapter(this)
        device_list_view.adapter = adapter
        device_list_view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = Intent()
            val address = adapter.getDevice(position)?.address
            intent.putExtra(DEVICE_ADDRESS_KEY, address)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        if(!btAdapter.isEnabled){
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            setResult(Activity.RESULT_CANCELED)
            Toast.makeText(applicationContext, "Bluetoothを有効にしてください", Toast.LENGTH_SHORT).show()
            finish()
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()

        if (btAdapter.isEnabled) {
            val leScanner = btAdapter.bluetoothLeScanner
            val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
            //val scanFilters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SDK_SERVICE_UUID)).build())
            leScanner.startScan(listOf(), scanSettings, callback)
        }
    }

    override fun onPause() {
        super.onPause()

        btAdapter.bluetoothLeScanner.stopScan(callback)

    }

}
