package com.example.wifidirectdemo

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MainActivity : ComponentActivity() {

    companion object {
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

    private lateinit var wifiReceiver: WifiP2PBroadcastReceiver


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

    private fun initWP2P() {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        chanel = manager.initialize(this, mainLooper, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWifiP2PPermissions()
        } else {
            Log.i("Permission", "Permission is not given.")
            return
        }

        wifiReceiver = WifiP2PBroadcastReceiver(manager, chanel, this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getWifiP2PPermissions() {
        if (!hasWifiP2PPermission()) {
            requestWifiP2PPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasWifiP2PPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    lateinit var sendReceive: SendReceive

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestWifiP2PPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PRC_ACCESS_FINE_lOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PRC_ACCESS_FINE_lOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Konum izni gereklidir", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun registerWifiReceiver() {
        registerReceiver(wifiReceiver, wifiFilter)
    }

    private fun unregisterWifiReceiver() {
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
        manager.discoverPeers(chanel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(MainActivity.TAG, "peer discover başarı ile başladı.")
            }

            override fun onFailure(p0: Int) {
                Log.d(MainActivity.TAG, "peer discover başarısız")
            }
        })
        val device = peerList[0]

        //her farklı bağlanılacak cihaza göre yapılacaklar ve config güncellenmeli
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(chanel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //success durumu yapılacaklar.
            }

            override fun onFailure(p0: Int) {
                Log.d(MainActivity.TAG, "peer connection hatası")
            }
        })


    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
            val serverClass = ServerClass()
            serverClass.start()
        } else if (info.groupFormed) {
            // The other device acts as the peer (client). In this case,
            // you'll want to create a peer thread that connects
            // to the group owner.
            val groupOwnerInetAddress: InetAddress = InetAddress.getByName(groupOwnerAddress)
            val clientClass = ClientClass(groupOwnerInetAddress)
            clientClass.start()
        }
    }

    //on peers change acion ile tetiklenen ve peer'ları listeleyen fonksiyon
    val peerListListener = WifiP2pManager.PeerListListener { peersList ->
        val refreshedPeers = peersList.deviceList
        if (refreshedPeers != peerList) {
            peerList.clear()
            // TODO: Should be updated with recording step to DB... (It should be optimized)
            peerList.addAll(refreshedPeers)
            //wifip2p ağına bağlanan yeni cihazda alınacak actionlar.
            onNewPeer()
        }
        if (peerList.isEmpty()) {
            Log.d(TAG, "bağlanan peer yok.")
        }
    }


    /*class P2pActionListener(private val purpose: String) : WifiP2pManager.ActionListener {
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
    }*/

    /*fun storePeers(peers:WifiP2pDeviceList){
        peers.apply {
            Log.v(TAG, "onPeersAvailable: $deviceList")

            peerList.apply {
                if (this != deviceList) {
                    clear()
                    addAll(deviceList)
                }
            }
        }
    }*/

    fun onNewPeer() {
        Toast.makeText(this, "yeni cihaz geldi", Toast.LENGTH_SHORT)
        // burası yeni gelen peer'a göre düzenlenecek.
    }

    /*@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
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
        manager.connect(chanel, config, object : WifiP2pManager.ActionListener{
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })
    }*/

    val MESSAGE_READ = 1
    val msg = Message()//karşıya gönderip alacağımız data

    val handler = Handler(Looper.getMainLooper())

    inner class ServerClass : Thread() {
        private lateinit var serverSocket: ServerSocket
        private lateinit var socket: Socket

        override fun run() {
            try {
                serverSocket = ServerSocket(8888)//dinlediği port
                socket = serverSocket.accept()
                sendReceive = SendReceive(socket)
                sendReceive.start()
            } catch (e: IOException) {
                Log.d(TAG, "server side socket olusturma hatası")
            }
        }
    }

    inner class SendReceive(val socket: Socket) : Thread() {
        private val inputStream = socket.getInputStream()
        private val outputStream = socket.getOutputStream()

        override fun run() {
            val buffer = ByteArray(1024)
            //var bytes = 0

            while (socket != null) {
                inputStream!!.read(buffer).let {
                    if (it > 0) {
                        handler.obtainMessage(
                            MESSAGE_READ, it, -1, buffer
                        ).sendToTarget()
                    }
                }
                Log.d(TAG, "SendReceive: Socket kontrol edildi")
            }
        }

        fun write(byteArray: ByteArray) {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    outputStream.write(byteArray)
                    Log.i(TAG, "write: Yazma başarılı")
                }
            }
        }
    }

    inner class ClientClass(inetAddress: InetAddress) : Thread() {
        private val socket = Socket()
        private val hostAddress: String = inetAddress.hostAddress

        override fun run() {
            try {
                socket.connect(
                    InetSocketAddress(hostAddress, 8888),
                    500
                ) // host'un 8888 portuna bağlanacağız - req time out 500
                sendReceive = SendReceive(socket)
                sendReceive.start()
            } catch (e: IOException) {
                Log.d(TAG, "client side socket olusturma hatası")
            }
        }
    }
}
