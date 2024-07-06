package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentMapBinding
import com.example.personalphysicaltracker.viewModels.SettingsViewModel
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

    private val settingsViewModel: SettingsViewModel = SettingsViewModel()

    private lateinit var map: MapView
    private lateinit var client: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var cancelLocation: Button

    private var currentGeofenceMarker: Marker? = null
    private var currentGeofenceKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root


        settingsViewModel.initializeGeofenceHandler(requireActivity())
        client = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(Context.MODE_PRIVATE))
        map = binding.map

        setMap()
        addMapClickListener()

        cancelLocation = root.findViewById(R.id.cancel_location_button)
        cancelLocation.setOnClickListener {
            cancelGeofence()
            findNavController().popBackStack()
        }
        showGeofence()

        return root
    }

    fun setGeofenceAt(point: GeoPoint) {
        if (settingsViewModel.checkLocationSet(requireActivity())) {
            showModifyGeofenceAlert(point)
        } else {
            setNewGeofence(point)
        }
    }

    private fun showModifyGeofenceAlert(point: GeoPoint) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Modify Geofence")
        alertDialogBuilder.setMessage("Do you want to modify the existing geofence location?")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, which ->
            setNewGeofence(point)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    private fun setNewGeofence(point: GeoPoint) {
        // Remove current geofence and marker if they exist
        currentGeofenceKey?.let { settingsViewModel.removeGeofence(it) }
        currentGeofenceMarker?.let { map.overlays.remove(it) }

        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = point.latitude
            longitude = point.longitude
        }
        val key = "geofence:${point.latitude};${point.longitude}"

        settingsViewModel.addGeofence(key,location)
        settingsViewModel.registerGeofence()

        settingsViewModel.editGeofencePreferences(key, location, requireActivity())

        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_add_location_24)
            title = "This is your point of interest"
        }
        map.overlays.add(marker)
        map.invalidate()

        currentGeofenceKey = key
        currentGeofenceMarker = marker
    }

    @SuppressLint("MissingPermission")
    private fun setMap() {
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
                    ContextCompat.getDrawable(requireContext(), R.drawable.baseline_adjust_24)
                startMarker.icon = icon
                startMarker.title = "You're here" // Set the marker title

                map.overlays.add(startMarker)
            }
        }
    }

    private fun addMapClickListener() {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
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

    private fun showGeofence() {
        // Remove previous marker if exists
        currentGeofenceMarker?.let { map.overlays.remove(it) }

        // Retrieve geofence information from SharedPreferences
        val key = settingsViewModel.obtainGeofenceKey(requireActivity())
        val latitude = settingsViewModel.obtainGeofenceLatitude(requireActivity())
        val longitude = settingsViewModel.obtainGeofenceLongitude(requireActivity())

        // Create and add marker of the geofence on the map
        val point = GeoPoint(latitude, longitude)
        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_add_location_24)
            title = "This is your point of interest"
        }
        map.overlays.add(marker)
        map.invalidate()

        // Update current geofence references
        currentGeofenceKey = key
        currentGeofenceMarker = marker

    }


    // Method to cancel the geofence
    private fun cancelGeofence() {
        // Remove geofence from SharedPreferences
        settingsViewModel.removeGeofencePreferences(requireActivity())

        // Remove marker from map if present
        currentGeofenceMarker?.let { map.overlays.remove(it) }
        map.invalidate()

        // Reset current geofence references
        currentGeofenceKey = null
        currentGeofenceMarker = null
    }
    override fun onResume() {
        super.onResume()
        map.onResume()
        showGeofence()
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


