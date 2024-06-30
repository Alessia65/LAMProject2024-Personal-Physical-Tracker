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
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentMapBinding
import com.example.personalphysicaltracker.handlers.GeofenceHandler
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
    private lateinit var cancelLocation: Button

    private var currentGeofenceMarker: Marker? = null
    private var currentGeofenceKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize components and services
        GeofenceHandler.initialize(requireContext())
        client = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Load map configuration
        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(Context.MODE_PRIVATE))
        map = binding.map
        setMap()
        addMapClickListener()


        // Set up cancel button listener
        cancelLocation = root.findViewById(R.id.cancel_location_button)
        cancelLocation.setOnClickListener {
            // Cancel the saved geofence
            cancelGeofence()

            // Navigate back to the settings fragment
            findNavController().popBackStack()
        }

        // Show the saved geofence on the map
        showGeofence()

        return root
    }

    // Method to set a geofence at a specific GeoPoint
    fun setGeofenceAt(point: GeoPoint) {
        // Check if there's an existing geofence
        if (checkLocation()) {
            // Show alert dialog to confirm modifying the geofence
            showModifyGeofenceAlert(point)
        } else {
            // No existing geofence, directly set the new one
            setNewGeofence(point)
        }
    }

    // Method to show alert dialog asking to modify existing geofence
    private fun showModifyGeofenceAlert(point: GeoPoint) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Modify Geofence")
        alertDialogBuilder.setMessage("Do you want to modify the existing geofence location? Attention: your data will be deleted")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, which ->
            // User wants to modify, set the new geofence
            setNewGeofence(point)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, which ->
            // User doesn't want to modify, do nothing
            dialog.dismiss()
        }
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    // Method to set a new geofence at the specified GeoPoint
    private fun setNewGeofence(point: GeoPoint) {
        // Remove current geofence and marker if they exist
        currentGeofenceKey?.let { GeofenceHandler.removeGeofence(it) }
        currentGeofenceMarker?.let { map.overlays.remove(it) }

        // Create location from GeoPoint
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = point.latitude
            longitude = point.longitude
        }
        val key = "geofence:${point.latitude};${point.longitude}"

        // Add geofence and register it
        GeofenceHandler.addGeofence(key, location)
        GeofenceHandler.registerGeofence()

        // Save geofence details to SharedPreferences
        saveGeofenceToPreferences(requireContext(), key, location)

        // Create and add marker to the map
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

    // Method to set up the map
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

    // Method to add click listener to the map
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

    // Method to show the saved geofence on the map
    private fun showGeofence() {
        // Remove previous marker if exists
        currentGeofenceMarker?.let { map.overlays.remove(it) }

        // Retrieve geofence information from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val key = sharedPreferences.getString(Constants.GEOFENCE_KEY, null)
        val latitude = sharedPreferences.getFloat(Constants.GEOFENCE_LATITUDE, 0.0f).toDouble()
        val longitude = sharedPreferences.getFloat(Constants.GEOFENCE_LONGITUDE, 0.0f).toDouble()

            // Create location from retrieved latitude and longitude
            val location = Location(LocationManager.GPS_PROVIDER).apply {
                this.latitude = latitude
                this.longitude = longitude
            }

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

    // Method to save geofence key and location to SharedPreferences
    private fun saveGeofenceToPreferences(context: Context, key: String, location: Location) {
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(Constants.GEOFENCE_KEY, key)
            putFloat(Constants.GEOFENCE_LATITUDE, location.latitude.toFloat())
            putFloat(Constants.GEOFENCE_LONGITUDE, location.longitude.toFloat())
            apply()
        }
    }

    // Method to check if there is an existing geofence
    private fun checkLocation(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val key = sharedPreferences.getString(Constants.GEOFENCE_KEY, null)
        return key != null
    }

    // Method to cancel the geofence
    private fun cancelGeofence() {
        // Remove geofence from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

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


