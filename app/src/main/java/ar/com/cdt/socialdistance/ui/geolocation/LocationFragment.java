package ar.com.cdt.socialdistance.ui.geolocation;


import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.List;
import ar.com.cdt.socialdistance.R;

/**
 * Created by Marcos Lozina on 19/05/2020. Pandemia Edition
 */

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = LocationFragment.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mPosicionCamara;
    private Location posicionActual;

    // The entry point to the Places API.
    private PlacesClient mLugaresCliente;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mLocalizacionProveidaClienteF;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mPosicionPredeterminada = new LatLng(-33.8523341, 151.2106085);
    private static final int ZOOM_PREDETERMINADO = 17;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mUltimaPosicionConocida;

    // Keys for storing activity state.
    private static final String LLAVE_POSICION_CAMARA = "camera_position";
    private static final String LLAVE_LOCALIZACION = "location";

    // Used for selecting the current place.
    private static final int M_ENTRADAS_MAXIMAS = 5;
    private String[] mNombresLugaresParecidos;
    private String[] mDireccionesLugaresParecidos;
    private List[] mAtributosLugaresParecidos;
    private LatLng[] mLatLongLugaresParecidos;


    SupportMapFragment fragmentoMapa;

    public LocationFragment(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_geolocalizacion, container, false);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mUltimaPosicionConocida = savedInstanceState.getParcelable(LLAVE_LOCALIZACION);
            mPosicionCamara = savedInstanceState.getParcelable(LLAVE_POSICION_CAMARA);
        }

        mLocalizacionProveidaClienteF = LocationServices.getFusedLocationProviderClient(getContext());
        ObtenerLocalizacion();

        return root;
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(LLAVE_POSICION_CAMARA, mMap.getCameraPosition());
            outState.putParcelable(LLAVE_LOCALIZACION, mUltimaPosicionConocida);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * llamada asincronica que se realiza cuando el mapa esta listo.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        LatLng latlang = new LatLng(posicionActual.getLatitude(), posicionActual.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latlang).title("mi posicion");
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlang));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlang, ZOOM_PREDETERMINADO));
        mMap.addMarker(markerOptions);
    }


    private void ObtenerLocalizacion()
    {
        Task<Location> task = mLocalizacionProveidaClienteF.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null)
                {
                    posicionActual = location;
                    SupportMapFragment fragmentoMapa = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                    assert fragmentoMapa != null;
                    fragmentoMapa.getMapAsync(LocationFragment.this);
                }
            }
        });
    }
}
