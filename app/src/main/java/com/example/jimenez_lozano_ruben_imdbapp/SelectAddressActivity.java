package com.example.jimenez_lozano_ruben_imdbapp;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.List;


public class SelectAddressActivity extends AppCompatActivity {
    private GoogleMap googleMap;
    private LatLng selectedLatLng;
    private String selectedAddress;
    private String fullAddress; // Dirección completa con código postal y provincia
    private Button btnConfirmAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_address);

        // Inicialización de Places
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_api_key));
        PlacesClient placesClient = Places.createClient(this);

        // Inicializa el fragmento de autocompletado
        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteSupportFragment != null) {
            Log.d("SelectAddressActivity", "AutocompleteSupportFragment inicializado correctamente");

            // Configurar los campos que queremos obtener
            autocompleteSupportFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS // Para obtener detalles adicionales
            ));

            // Configurar el listener del fragmento de autocompletado
            autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    selectedLatLng = place.getLatLng();
                    selectedAddress = place.getName();

                    // Obtener detalles completos del lugar usando Place Details
                    fetchPlaceDetails(place.getId(), placesClient);
                }

                @Override
                public void onError(Status status) {
                    Log.e("SelectAddressActivity", "Error en la selección de lugar: " + status.getStatusMessage());
                    Toast.makeText(SelectAddressActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("SelectAddressActivity", "Error al inicializar AutocompleteSupportFragment");
        }

        // Inicializa el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                this.googleMap = googleMap;
                googleMap.getUiSettings().setZoomControlsEnabled(true);
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            });
        }

        // Configurar el botón de confirmación
        btnConfirmAddress = findViewById(R.id.btnConfirmAddress);
        btnConfirmAddress.setOnClickListener(v -> confirmAddress());
    }

    private void fetchPlaceDetails(String placeId, PlacesClient placesClient) {
        // Solicitar detalles adicionales del lugar
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();

            // Procesar los componentes de la dirección
            StringBuilder addressBuilder = new StringBuilder();
            for (AddressComponent component : place.getAddressComponents().asList()) {
                for (String type : component.getTypes()) {
                    switch (type) {
                        case "street_number":
                        case "route":
                            addressBuilder.append(component.getName()).append(", ");
                            break;
                        case "locality":
                            addressBuilder.append(component.getName()).append(", ");
                            break;
                        case "administrative_area_level_1": // Provincia
                            addressBuilder.append(component.getName()).append(", ");
                            break;
                        case "postal_code": // Código postal
                            addressBuilder.append(component.getShortName()).append(", ");
                            break;
                        case "country":
                            addressBuilder.append(component.getName());
                            break;
                    }
                }
            }

            // Guardar la dirección completa
            fullAddress = addressBuilder.toString().trim();
            Log.d("SelectAddressActivity", "Dirección completa: " + fullAddress);



            // Actualizar el mapa
            selectedLatLng = place.getLatLng();
            if (googleMap != null && selectedLatLng != null) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(selectedLatLng).title(fullAddress));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }
        }).addOnFailureListener(exception -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e("SelectAddressActivity", "Error en Place Details: " + apiException.getMessage());
                Toast.makeText(SelectAddressActivity.this, "Error: " + apiException.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmAddress() {
        if (selectedLatLng != null && fullAddress != null) {
            Intent intent = new Intent();
            intent.putExtra("address", fullAddress);
            intent.putExtra("latitude", selectedLatLng.latitude);
            intent.putExtra("longitude", selectedLatLng.longitude);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, "Por favor, selecciona una dirección primero.", Toast.LENGTH_SHORT).show();
        }
    }
}