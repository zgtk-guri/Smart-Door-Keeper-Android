package net.gurigoro.smart_door_keyper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.PRIORITY_MIN
import com.deploygate.sdk.DeployGate
import java.util.*

class BleConnectService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    var gatt: BluetoothGatt? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = getString(R.string.app_name)

        val channelName = "ステータス表示"
        val channelId = "status_channel"
        val channelDescription = "BLE接続の状態を表示します"
        val notificationId = 1

        DeployGate.logInfo("Service On Start Command.")

        if(notificationManager.getNotificationChannel(channelId) == null){
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)
            channel.apply {
                description = channelDescription

            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
                .apply {
                    setSmallIcon(R.drawable.ic_locked_24dp)
                    mContentTitle = "Smart Door Keeper"
                    mContentText = "BLE未接続です。"
                    priority = PRIORITY_MIN
                }.build()
        startForeground(notificationId, notification)

        Thread(
                Runnable {
                    val sharedPreferences = getSharedPreferences("Smart-Door-Keyper", Context.MODE_PRIVATE)
                    val targetAddress = sharedPreferences.getString(getString(R.string.pref_ble_addr), "")
                    val targetSecret = sharedPreferences.getString(getString(R.string.pref_secret), "")

                    val btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

                    val device = btAdapter.getRemoteDevice(targetAddress) ?: kotlin.run {
                        stopForeground(Service.STOP_FOREGROUND_REMOVE)
                        return@Runnable
                    }

                    val callback = object : BluetoothGattCallback(){
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            if(newState == BluetoothAdapter.STATE_CONNECTED){
                                gatt?.discoverServices()
                                val newNotification = NotificationCompat.Builder(this@BleConnectService, channelId).apply {
                                    setSmallIcon(R.drawable.ic_locked_24dp)
                                    mContentTitle = "Smart Door Keeper"
                                    mContentText = "接続中です。"
                                    priority = PRIORITY_MIN
                                }.build()
                                notificationManager.notify(notificationId, newNotification)
                                this@BleConnectService.gatt = gatt
                                DeployGate.logDebug("BLE Connected.")
                            }
                            if(newState == BluetoothAdapter.STATE_DISCONNECTED){
                                val newNotification = NotificationCompat.Builder(this@BleConnectService, channelId)
                                        .apply {
                                            setSmallIcon(R.drawable.ic_locked_24dp)
                                            mContentTitle = "Smart Door Keeper"
                                            mContentText = "BLE未接続です。"
                                            priority = PRIORITY_MIN
                                        }.build()
                                notificationManager.notify(notificationId, newNotification)
                                this@BleConnectService.gatt = null

                                Thread.sleep(5000)

                                device.connectGatt(applicationContext, false, this)
                                DeployGate.logDebug("BLE Disconnect. Retry.")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            val sdkService = gatt?.getService(UUID.fromString("71495a29-c548-4aa6-a75b-61959767faa2"))
                            val secretCharacteristic = sdkService?.getCharacteristic(UUID.fromString("1775cd75-a329-4607-a652-9a0a69ba77c8")) ?:kotlin.run {
                                gatt?.disconnect()
                                return
                            }

                            secretCharacteristic.value = targetSecret.toByteArray()
                            gatt.writeCharacteristic(secretCharacteristic)

                            val newNotification = NotificationCompat.Builder(this@BleConnectService, channelId).apply {
                                setSmallIcon(R.drawable.ic_unlocked_24dp)
                                mContentTitle = "Smart Door Keeper"
                                mContentText = "BLE接続済みです。"
                                priority = PRIORITY_MIN
                            }.build()
                            notificationManager.notify(notificationId, newNotification)
                            DeployGate.logDebug("BLE Characteristic written.")
                        }
                    }

                    device.connectGatt(applicationContext, false, callback)

                }
        ).start()

        return START_STICKY
    }

    override fun onDestroy() {
        gatt?.close()
        DeployGate.logInfo("Service on Destroy.")
        super.onDestroy()
    }
}
