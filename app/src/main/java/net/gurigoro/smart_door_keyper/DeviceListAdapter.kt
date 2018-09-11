package net.gurigoro.smart_door_keyper

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DeviceListAdapter(context: Context): BaseAdapter() {
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private val inflater = LayoutInflater.from(context)


    fun addDevice(device: BluetoothDevice){
        if (deviceList.count { d -> d.address.equals(device.address) } == 0) {
            deviceList.add(device)
        }else{
            deviceList.set(deviceList.indexOfFirst { d -> d.address.equals(device.address) }, device)
        }
    }

    fun getDevice(position: Int): BluetoothDevice?{
        return deviceList[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.adapter_device_list_item, parent, false)
        val device = deviceList[position]
        val nameText = view.findViewById<TextView>(R.id.device_name_text)
        val infoText = view.findViewById<TextView>(R.id.device_info_list)


        nameText.text = device.name
        infoText.text = device.address

        return view
    }

    override fun getItem(p0: Int): Any {
        return deviceList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return deviceList.count()
    }
}