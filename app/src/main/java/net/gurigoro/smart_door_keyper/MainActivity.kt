package net.gurigoro.smart_door_keyper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    lateinit var sharedPref : SharedPreferences

    companion object {
        const val SCAN_ACTIVITY_RESULT_CODE = 254
        const val REQUEST_PERMISSION_REQUEST_CODE = 234
    }

    var available = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = this.getSharedPreferences("Smart-Door-Keyper", Context.MODE_PRIVATE)

        available = true
        if(sharedPref.contains(getString(R.string.pref_secret))){
            val text = getString(R.string.secret_key_setting_status) + ": 設定済"
            main_secret_status_label.text = text
        }else{
            val text = getString(R.string.secret_key_setting_status) + ": 未設定"
            main_secret_status_label.text = text
            available = false
        }

        if(sharedPref.contains(getString(R.string.pref_ble_addr))){
            val addr = sharedPref.getString(getString(R.string.pref_ble_addr), "")
            val text = getString(R.string.connect_to_address) + ": " + addr
            main_address_text.text = text
        }else{
            val text = getString(R.string.connect_to_address) + ": 未設定"
            main_address_text.text = text
            available = false
        }

        if(available){
            startService()
        }

        main_secret_set_button.setOnClickListener {
            if(main_secret_edit_text.text.length == 20){
                sharedPref.edit().putString(getString(R.string.pref_secret), main_secret_edit_text.text.toString()).apply()
                val text = getString(R.string.secret_key_setting_status) + ": 設定済"
                main_secret_status_label.text = text
                if(sharedPref.contains(getString(R.string.pref_ble_addr))){
                    startService()
                }
            }
        }

        main_ble_scan_button.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, SCAN_ACTIVITY_RESULT_CODE)
        }

        if(checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSION_REQUEST_CODE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SCAN_ACTIVITY_RESULT_CODE -> {
                if(resultCode == Activity.RESULT_OK) {
                    val addr = data?.getStringExtra(ScanActivity.DEVICE_ADDRESS_KEY)
                    sharedPref.edit().putString(getString(R.string.pref_ble_addr), addr).apply()
                    val text = getString(R.string.connect_to_address) + ": " + addr
                    main_address_text.text = text
                    if (sharedPref.contains(getString(R.string.pref_secret))) {
                        startService()
                    }
                }
            }
            else -> {
            }
        }
    }

    fun startService(){
        val intent = Intent(this, BleConnectService::class.java)
        stopService(intent)
        startForegroundService(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            REQUEST_PERMISSION_REQUEST_CODE -> {
                if(grantResults.count() == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSION_REQUEST_CODE)
                }
            }
        }
    }
}
