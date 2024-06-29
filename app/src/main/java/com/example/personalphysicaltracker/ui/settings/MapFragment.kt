package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.personalphysicaltracker.PermissionsHandler
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.MapEventsOverlay

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private lateinit var client: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var setLocation: Button
    private lateinit var cancelLocation: Button

    private var currentGeofenceMarker: Marker? = null
    private var currentGeofenceKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        GeofenceHandler.initialize(requireContext())
        client = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(Context.MODE_PRIVATE))
        map = binding.map
        setMap()
        addMapClickListener()

        setLocation = root.findViewById(R.id.set_location_button)
        cancelLocation = root.findViewById(R.id.cancel_location_button)

        return root
    }


    @SuppressLint("MissingPermission")
    fun setMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)

        client.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val startPoint = GeoPoint(it.latitude, it.longitude)
                val mapController = map.controller
                mapController.setZoom(18.0)
                mapController.setCenter(startPoint)

                val startMarker = Marker(map)
                startMarker.position = startPoint
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val icon: Drawable? =
                    ContextCompat.getDrawable(requireContext(), R.drawable.baseline_add_location_24)
                startMarker.icon = icon

                map.overlays.add(startMarker)
            }
        }
    }

    fun addMapClickListener() {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.d("Map Click", "Tapped at: ${p.latitude}, ${p.longitude}")
                setGeofenceAt(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        val overlay = MapEventsOverlay(receiver)
        map.overlays.add(overlay)
    }

    fun setGeofenceAt(point: GeoPoint) {
        // Remove the current geofence and marker if they exist
        currentGeofenceKey?.let { GeofenceHandler.removeGeofence(it) }
        currentGeofenceMarker?.let { map.overlays.remove(it) }

        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = point.latitude
            longitude = point.longitude
        }
        val key = "geofence_${point.latitude}_${point.longitude}"
        GeofenceHandler.addGeofence(key, location)
        GeofenceHandler.registerGeofence()

        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_add_location_24)
            setOnMarkerClickListener { marker, mapView ->
                removeGeofenceAt(marker.position)
                mapView.overlays.remove(marker)
                mapView.invalidate()
                true
            }
        }
        map.overlays.add(marker)
        map.invalidate()

        // Update the current geofence and marker references
        currentGeofenceKey = key
        currentGeofenceMarker = marker
    }

    fun removeGeofenceAt(point: GeoPoint) {
        val key = "geofence_${point.latitude}_${point.longitude}"
        GeofenceHandler.removeGeofence(key)
        GeofenceHandler.registerGeofence()  // Re-register to update the geofences
        currentGeofenceMarker = null
        currentGeofenceKey = null
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // Save the geofence key and location to SharedPreferences
    private fun saveGeofenceToPreferences(context: Context, key: String, location: Location) {
        val sharedPreferences = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("geofence_key", key)
            putString("geofence_latitude", location.latitude.toString())
            putString("geofence_longitude", location.longitude.toString())
            apply()
        }
    }
}
