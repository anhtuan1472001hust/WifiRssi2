package com.example.rssiwifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rssiwifi.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: RssiAdapter
    private lateinit var wifiManager: WifiManager
    private lateinit var database: FirebaseDatabase
    private lateinit var locationsRef: DatabaseReference
    private lateinit var demoRef: DatabaseReference
    private lateinit  var edtRoom: String
    private lateinit var edtLabel: String
    private var numberClick = 0

    companion object {
        const val REQUEST_CODE_PERMISSION_LOCATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        registerBroadcastReceiver()
        setMainAdapter()
        addListSpinner()
        database = FirebaseDatabase.getInstance()
        locationsRef = database.getReference("locations")
        demoRef = database.getReference("demo")
        edtRoom = binding.edtRoom.text.toString()
        edtLabel = binding.edtLatitude.text.toString()
        setButtonClick()
    }

    private fun setButtonClick() {
        binding.buttonScan.setOnClickListener {
            scanWifi()
        }
        binding.buttonSave.setOnClickListener {
            numberClick++
            if (numberClick > 1) {
                val newRoom = binding.edtRoom.text.toString()
                val newLabel = binding.edtLatitude.text.toString()
                val newData = "${binding.edtLatitude.text}:${binding.edtLongitude.text}"
                Log.e("Bello","newRoom: $newRoom")
                Log.e("Bello","oldRoom: $edtRoom")
                if (newRoom != edtRoom || newLabel != edtLabel) {
                    numberClick = 1
                }
            }
            val latitude = binding.edtLatitude.text.toString()
            val longitude = binding.edtLongitude.text.toString()
            uploadLocationData(
                WifiScan(
                    mainAdapter.getItemSelected(),
                    latitude = latitude
                ),
                binding.edtRoom.text.toString()
            )
        }
    }

    private fun setMainAdapter() {
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mainAdapter = RssiAdapter()
        binding.rcvWifi.adapter = mainAdapter
        binding.rcvWifi.layoutManager = linearLayoutManager
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_PERMISSION_LOCATION
        )
    }

    private fun registerBroadcastReceiver() {
        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                } else {
                    false
                }
                if (success) {
                    Log.e("Bello","receive update scan result")
                    scanResult()
                }
            }

        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        applicationContext.registerReceiver(wifiScanReceiver, intentFilter)
    }

    private fun scanWifi() {
        wifiManager.startScan()
        scanResult()
    }

    private fun scanResult() {
        val results = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            null
        } else {
            wifiManager.scanResults
        }
        val filterResult = results?.filter { !it.SSID.isNullOrEmpty() }
        val listWifiModel = arrayListOf<WifiModel>()
        for (result in filterResult!!) {
            listWifiModel.add(WifiModel(wifiName = result.SSID, wifiMac = result.BSSID, wifiRssi = result.level))
        }
        mainAdapter.submitList(listWifiModel)
    }

    private fun addListSpinner() {
        val directions = arrayOf<String?>("Đông", "Tây", "Nam", "Bắc")
        val arrayAdapter: ArrayAdapter<*> = ArrayAdapter<Any?> (
            this,
            android.R.layout.simple_spinner_item,
            directions
        )
//        binding.spinnerDirection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onNothingSelected(p0: AdapterView<*>?) {
//                TODO("Not yet implemented")
//            }
//
//        }
    }

    private fun uploadLocationData(wifiScan: WifiScan, room: String) {
        edtRoom = room
        edtLabel = binding.edtLatitude.text.toString()
        for (wifi in wifiScan.listWifi) {
            wifi.wifiName?.let {
                demoRef.child(room).child(wifiScan.latitude.toString()).child(it).child(numberClick.toString()).setValue(wifi.wifiRssi)
                    .addOnSuccessListener {
                        // Xử lý khi ghi dữ liệu thành công
                    }
                    .addOnFailureListener {
                        // Xử lý khi ghi dữ liệu thất bại
                    }
            }
        }
    }

    fun averageOfList(list: List<Int>): Double {
        if (list.isEmpty()) return 0.0

        val sum = list.sum()

        return sum.toDouble() / list.size
    }

//    private fun getPreviousData(wifiScan: WifiScan) {
//        val reference = demoRef.child("${wifiScan.latitude}:${wifiScan.longitude}").child()
//        reference.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    for (childSnapshot in snapshot.children) {
//                        val wifiName = childSnapshot.key
//                        val wifiRssi = childSnapshot.value
//                        for (wifi in wifiScan.listWifi) {
//                            if (wifi.wifiName == wifiName) {
//                                wifi.wifiRssi =
//                            }
//                        }
//                    }
//                    val data = snapshot.value
//                } else {
//                    uploadLocationData(wifiScan)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })
//    }
}