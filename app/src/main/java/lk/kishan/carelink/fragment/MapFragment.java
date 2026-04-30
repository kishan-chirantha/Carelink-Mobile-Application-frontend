package lk.kishan.carelink.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.BuildConfig;
import lk.kishan.carelink.R;
import lk.kishan.carelink.databinding.FragmentMapBinding;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LatLng selectedLocation;
    private Marker currentMarker;

    private final ActivityResultLauncher<Intent> searchResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    LatLng latLng = place.getLocation();

                    if (latLng != null) {
                        placeMarkerAndGetAddress(latLng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    String errorMessage = status != null ? status.getStatusMessage() : "Unknown Error";
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
    );

    public MapFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY);
        }

        binding.mapSearchCard.setOnClickListener(v -> {

            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountries(Arrays.asList("LK"))
                    .build(requireContext());
            searchResultLauncher.launch(intent);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.locationConfirmBottomSheet.setVisibility(View.VISIBLE);
        binding.mapSelectedAddressText.setText("Tap on the map to select your delivery location");


        binding.mapConfirmLocation.setOnClickListener(v -> {

            if (selectedLocation != null) {

                double selectedLat = selectedLocation.latitude;
                double selectedLng = selectedLocation.longitude;
                String addressText = binding.mapSelectedAddressText.getText().toString();

                Bundle bundle = new Bundle();
                bundle.putDouble("USER_LAT", selectedLat);
                bundle.putDouble("USER_LNG", selectedLng);
                bundle.putString("USER_ADDRESS", addressText);

                PharmacyListFragment fragment = new PharmacyListFragment();
                fragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();

            } else {
                Toast.makeText(getContext(), "Please select delivery location first.", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        int topPadding = (int) (100 * getResources().getDisplayMetrics().density);
        mMap.setPadding(0, topPadding, 0, 0);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        enableUserLocation();

        mMap.setOnMapClickListener(latLng -> {
            placeMarkerAndGetAddress(latLng);
        });
    }

    private void enableUserLocation() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            mMap.setOnMyLocationButtonClickListener(() -> {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        placeMarkerAndGetAddress(latLng);

                    } else {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(currentLocation -> {
                            if (currentLocation != null) {
                                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                placeMarkerAndGetAddress(latLng);
                            } else {
                                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                return false;
            });

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                 if (!isAdded() || binding == null) return;

                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    placeMarkerAndGetAddress(currentLatLng);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                }
            });
            startLocationUpdate();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void placeMarkerAndGetAddress(LatLng latLng) {
        if (!isAdded() || mMap == null) return;

        selectedLocation = latLng;
        if (currentMarker != null) {
            currentMarker.setPosition(latLng);
        } else {
            currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Delivery Location"));
        }
        getAddressFromLatLng(latLng);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void startLocationUpdate() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).setMinUpdateIntervalMillis(5000).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

            }

        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();

            } else {
                Toast.makeText(getContext(), "Permission denied! Please select location manually.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getAddressFromLatLng(LatLng latLng) {
        if (!isAdded() || binding == null) return;

        binding.mapSelectedAddressText.setText("Loading address...");
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                binding.mapSelectedAddressText.setText(address);

            } else {
                binding.mapSelectedAddressText.setText("Unknown Location");
            }

        } catch (IOException e) {
            binding.mapSelectedAddressText.setText("Selected delivery location");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        View addPrescriptionBtn = requireActivity().findViewById(R.id.addPrescriptionButton);
        if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.GONE);

        View fragmentContainer = requireActivity().findViewById(R.id.main_fragment_container);
        if (fragmentContainer != null) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            layoutParams.bottomMargin = 0;
            fragmentContainer.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        View bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        View addPrescriptionBtn = requireActivity().findViewById(R.id.addPrescriptionButton);
        if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.VISIBLE);

        View fragmentContainer = requireActivity().findViewById(R.id.main_fragment_container);
        if (fragmentContainer != null) {
            int marginInPx = (int) (60 * getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            layoutParams.bottomMargin = marginInPx;
            fragmentContainer.setLayoutParams(layoutParams);
        }

        binding = null;
    }
}