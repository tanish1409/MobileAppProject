package com.example.network

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_location)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.selectLocationMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Back button
        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // Confirm button
        findViewById<Button>(R.id.confirmLocationBtn).setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please choose a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent()
            intent.putExtra("lat", selectedLatLng!!.latitude)
            intent.putExtra("lng", selectedLatLng!!.longitude)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map

        val defaultLocation = LatLng(43.6532, -79.3832) // Toronto
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Enable map gestures
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isTiltGesturesEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = true

        // Tap to place marker
        mMap.setOnMapClickListener { point ->
            selectedLatLng = point

            marker?.remove()
            marker = mMap.addMarker(
                MarkerOptions().position(point).title("Selected Location")
            )

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16f))
        }
    }
}
