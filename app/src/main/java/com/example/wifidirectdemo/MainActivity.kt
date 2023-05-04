package com.example.wifidirectdemo

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.wifidirectdemo.ui.theme.WifiDirectDemoTheme

class MainActivity : ComponentActivity() {

    companion object{
        val TAG = MainActivity::class.java.simpleName
    }

    val PRC_ACCESS_FINE_lOCATION = 1
    //Wifi alıcısı için filtreleme
    private val wifiFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    // Wifi değişikliklerinde receiver'ı çalıştırma
    private lateinit var manager: WifiP2pManager
    // wifi p2p framework ile uygulamayı bağlayacak obje
    private lateinit var chanel: WifiP2pManager.Channel

    private lateinit var wifiReceiver : WifiP2PBroadcastReceiver



    var p2pEnable = false
        set(value) {
            field = value
        }

    val peerList = ArrayList<WifiP2pDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWP2P()
    }
    private fun initWP2P(){
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        chanel = manager.initialize(this,mainLooper,null)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            getWifiP2PPermissions()
        }else{
            return
        }

        wifiReceiver  = WifiP2PBroadcastReceiver(manager, chanel,this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getWifiP2PPermissions(){
        if(!hasWifiP2PPermission()){
            requestWifiP2PPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasWifiP2PPermission(): Boolean{
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestWifiP2PPermissions(){
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PRC_ACCESS_FINE_lOCATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PRC_ACCESS_FINE_lOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Konum izni gereklidir",Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun registerWifiReceiver(){
        registerReceiver(wifiReceiver,wifiFilter)
    }
    private fun unregisterWifiReceiver(){
        unregisterReceiver(wifiReceiver)
    }
    override fun onResume() {
        super.onResume()
        registerWifiReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterWifiReceiver()
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onDiscoverButtonClicked(view: View) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
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
        manager.discoverPeers(chanel,P2pActionListener("keşfet"))
    }

    class P2pActionListener(private val purpose: String) : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "onSuccess: $purpose başarılı")
        }

        override fun onFailure(reason: Int) {
            val reasonMsg = when (reason) {
                WifiP2pManager.P2P_UNSUPPORTED -> "P2P desteklenmiyor"
                WifiP2pManager.ERROR -> "hata oluştur"
                WifiP2pManager.BUSY -> "cihaz başka bir bağlantı ile meşgul"
                else -> ""
            }

            Log.e(TAG, "onDiscoverButtonClick: $purpose başarısız, $reasonMsg")
        }
    }

    fun storePeers(peers:WifiP2pDeviceList){
        peers.apply {
            Log.v(TAG, "onPeersAvailable: $deviceList")

            peerList.apply {
                if (this != deviceList) {
                    clear()
                    addAll(deviceList)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun connectPeer(peer: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = peer.deviceAddress
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
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
        manager.connect(chanel, config, P2pActionListener("Bağlantı"))
    }

    fun createSocket(info: WifiP2pInfo){
        when{
            isServer(info) -> { /*server socket*/}
            isClient(info) -> {/*server socket*/}
        }
    }

    private fun isServer(info: WifiP2pInfo):Boolean{
        return info.run(){
            groupFormed && isGroupOwner
        }
    }

    private fun isClient(info: WifiP2pInfo):Boolean{
        return info.run(){
            groupFormed && !isGroupOwner
        }
    }
}
