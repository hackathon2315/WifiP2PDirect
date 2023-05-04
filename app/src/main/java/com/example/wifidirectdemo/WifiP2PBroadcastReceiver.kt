package com.example.wifidirectdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat

class WifiP2PBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val wifiP2pActivity: MainActivity
) : BroadcastReceiver() {

    companion object{
        val TAG = WifiP2PBroadcastReceiver::class.java.simpleName
    }
    //durum değişikliğinde çalışan method

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityCompat.checkSelfPermission(
                wifiP2pActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (ActivityCompat.checkSelfPermission(
                wifiP2pActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> onStateChanged(intent)
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> onPeerChanged()
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION ->
                onConnectionChanged()
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ->
                onThisDeviceChanged()
        }
    }

    private fun onStateChanged(intent: Intent): Unit {
        Log.d(TAG, "onStateChanged: Wifi P2P durumu değişti")
        wifiP2pActivity.p2pEnable = when (
            intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
        ) {
            WifiP2pManager.WIFI_P2P_STATE_ENABLED -> true
            else -> false
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun onPeerChanged(): Unit {
        Log.d(TAG, "onPeerChanged: WiFi eşleri değişti")


        if (ActivityCompat.checkSelfPermission(
                wifiP2pActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                wifiP2pActivity,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        manager.requestPeers(channel, wifiP2pActivity::storePeers)
    }

    private fun onConnectionChanged(): Unit {
        Log.d(TAG, "onConnectionChanged: WiFi P2P bağlantısı değişti")

        manager.requestConnectionInfo(channel, wifiP2pActivity::createSocket)
    }

    private fun onThisDeviceChanged(): Unit {
        Log.d(TAG, "onThisDeviceChanged: Cihazın WiFi P2P durumu değişti")
    }

}