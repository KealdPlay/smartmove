package com.example.smartmovecx;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    
    private EditText txtLat, txtLng;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;
    
    // Path A and Path B variables
    private List<LatLng> pathAPoints;
    private List<LatLng> pathBPoints;
    private Polyline pathAPolyline;
    private Polyline pathBPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize text fields
        txtLat = findViewById(R.id.txtLat);
        txtLng = findViewById(R.id.txtLng);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize paths
        pathAPoints = new ArrayList<>();
        pathBPoints = new ArrayList<>();
        
        // Initialize location update handler
        locationUpdateHandler = new Handler(Looper.getMainLooper());
        
        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            setupMap();
        }
        
        // Define Path A and Path B
        definePathA();
        definePathB();
    }
    
    private void definePathA() {
        // DEFINE HERE - Path A points will be defined later
        // Example format: 
        // pathAPoints.add(new LatLng(latitude, longitude));
        
        // Once defined, draw the path on the map
        drawPath(pathAPoints, true);
    }
    
    private void definePathB() {
        // DEFINE HERE - Path B points will be defined later
        // Example format: 
        // pathBPoints.add(new LatLng(latitude, longitude));
        
        // Once defined, draw the path on the map
        drawPath(pathBPoints, false);
    }
    
    private void drawPath(List<LatLng> points, boolean isPathA) {
        if (points.isEmpty() || mMap == null) return;
        
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(12)
                .color(isPathA ? getResources().getColor(android.R.color.holo_blue_dark) : 
                                getResources().getColor(android.R.color.holo_green_dark))
                .geodesic(true);
        
        if (isPathA) {
            if (pathAPolyline != null) {
                pathAPolyline.remove();
            }
            pathAPolyline = mMap.addPolyline(polylineOptions);
        } else {
            if (pathBPolyline != null) {
                pathBPolyline.remove();
            }
            pathBPolyline = mMap.addPolyline(polylineOptions);
        }
    }

    @SuppressLint("MissingPermission")
    private void setupMap() {
        mMap.setMyLocationEnabled(true);
        
        // Start periodic location updates
        startLocationUpdates();
        
        // Get initial location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            updateLocationDisplay(location);
                        } else {
                            // Default location if unable to get current location
                            LatLng mexico = new LatLng(21.88234, -102.28259);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mexico, 10.2f));
                        }
                    }
                });
    }
    
    private void startLocationUpdates() {
        locationUpdateRunnable = new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    updateLocationDisplay(location);
                                }
                                // Schedule the next update
                                locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL);
                            }
                        });
            }
        };
        
        // Start the initial update
        locationUpdateHandler.post(locationUpdateRunnable);
    }
    
    private void updateLocationDisplay(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        
        // Update UI with current coordinates
        txtLat.setText(String.valueOf(location.getLatitude()));
        txtLng.setText(String.valueOf(location.getLongitude()));
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        txtLat.setText("" + latLng.latitude);
        txtLng.setText("" + latLng.longitude);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        txtLat.setText("" + latLng.latitude);
        txtLng.setText("" + latLng.longitude);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when app is in background
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Restart location updates when app is in foreground
        if (locationUpdateHandler != null && locationUpdateRunnable != null &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationUpdateHandler.post(locationUpdateRunnable);
        }
    }
}