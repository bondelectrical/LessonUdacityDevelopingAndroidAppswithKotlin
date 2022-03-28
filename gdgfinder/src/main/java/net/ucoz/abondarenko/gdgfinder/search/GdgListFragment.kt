package net.ucoz.abondarenko.gdgfinder.search

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import net.ucoz.abondarenko.gdgfinder.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import net.ucoz.abondarenko.gdgfinder.databinding.FragmentGdgListBinding
import com.google.android.gms.location.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

private const val LOCATION_PERMISSION_REQUEST = 1

private const val LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION"

class GdgListFragment : Fragment() {


    private val viewModel: GdgListViewModel by lazy {
        ViewModelProvider(this).get(GdgListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentGdgListBinding.inflate(inflater)

        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.setLifecycleOwner(this)

        // Giving the binding access to the OverviewViewModel
        binding.viewModel = viewModel

        val adapter = GdgListAdapter(GdgClickListener { chapter ->
            val destination = Uri.parse(chapter.website)
            startActivity(Intent(Intent.ACTION_VIEW, destination))
        })

        // Sets the adapter of the RecyclerView
        binding.gdgChapterList.adapter = adapter

        viewModel.showNeedLocation.observe(viewLifecycleOwner, object: Observer<Boolean> {
            override fun onChanged(show: Boolean?) {
                // Snackbar is like Toast but it lets us show forever
                if (show == true) {
                    Snackbar.make(
                        binding.root,
                        "No location. Enable location in settings (hint: test with Maps) then check app permissions!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        })

        viewModel.regionList.observe(viewLifecycleOwner, object: Observer<List<String>> {
            override fun onChanged(data: List<String>?) {
                data ?: return
                val chipGroup = binding.regionList
                val inflator = LayoutInflater.from(chipGroup.context)

                val children = data.map { regionName ->
                    val chip = inflator.inflate(R.layout.region, chipGroup, false) as Chip
                    chip.text = regionName
                    chip.tag = regionName
                    chip.setOnCheckedChangeListener { button, isChecked ->
                        viewModel.onFilterChanged(button.tag as String, isChecked)
                    }
                    chip
                }

                chipGroup.removeAllViews()

                for (chip in children) {
                    chipGroup.addView(chip)
                }
            }
        })

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLastLocationOrStartLocationUpdates()
    }


    private fun requestLocationPermission() {
        requestPermissions(arrayOf(LOCATION_PERMISSION), LOCATION_PERMISSION_REQUEST)
    }


    private fun requestLastLocationOrStartLocationUpdates() {
        // if we don't have permission ask for it and wait until the user grants it
        if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                startLocationUpdates(fusedLocationClient)
            } else {
                viewModel.onLocationUpdated(location)
            }
        }
    }

    private fun startLocationUpdates(fusedLocationClient: FusedLocationProviderClient) {
        // if we don't have permission ask for it and wait until the user grants it
        if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }


        val request = LocationRequest().setPriority(LocationRequest.PRIORITY_LOW_POWER)
        val callback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult?.lastLocation ?: return
                viewModel.onLocationUpdated(location)
            }
        }
//        fusedLocationClient.requestLocationUpdates(request, callback, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLastLocationOrStartLocationUpdates()
                }
            }
        }
    }
}


