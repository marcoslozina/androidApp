package ar.com.cdt.socialdistance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import ar.com.cdt.socialdistance.bluetoothHelper.BluetoothHelper;
import ar.com.cdt.socialdistance.ui.adapters.BeaconsListAdapter;
import ar.com.cdt.socialdistance.ui.controls.BeaconControls;

import static ar.com.cdt.socialdistance.ui.bluetooth.BluetoothDistanceFragment.getUUID;

/**
 * Created by Marcos Lozina on 18/05/2020. Pandemia Edition
 */


public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    ///region DECLARACION DE VARIABLES

//    private static Context context;
    int TODOS_LOS_PERMISOS = 1;
    String[] PERMISOS = {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
    };
    private Button btnComenzarLecturaBeacons, btnDetenerLecturaBeacons;
    private ListView lvBeacons;
    private TextView tvDispositivosEncontrados;
    private MediaPlayer sonidoAlert;
    private Vibrator vibracion;

    protected final String TAG = "Distancia Social";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final long DEFAULT_SCAN_PERIOD_MS = 2000l;
    private static final long DEFAULT_VIBRATE_PERIOD_MS = 1000l;
    private static final String ALL_BEACONS_REGION = "AllBeaconsRegion";

    private static final String VACIO = "";
    private static final String OPCION_1 = "TRUE";
    private static final String OPCION_2 = "FALSE";

    // Para interactuar con los beacons desde una actividad
    private BeaconManager mBeaconManager;

    // Representa el criterio de campos con los que buscar beacons
    private Region mRegion;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;

    ArrayList<BeaconControls> mArrayBeacons;
    BeaconsListAdapter mBeaconsListAdapter;
    ///////////////////////////////Transmisor///////////////////////////
    BluetoothHelper bluetoothHelper;
    boolean isBluetoothEnabled;
    Beacon beacon;
    BeaconParser beaconParser;
    BeaconTransmitter beaconTransmitter;
    private BeaconManager beaconManager;
    int beaconLayout = 0;
    String[] beaconFormat = {"AltBeacon", "iBeacon"};
    String[] parserLayout = {"m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25", "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"};
    String currentuuid, currentmajorValue, currentminorValue;
    int currentType;
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    ///////////////////////////////////////////////////////////////

    ///endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!PermisosGarantizados(this, PERMISOS)) ActivityCompat.requestPermissions(this, PERMISOS, TODOS_LOS_PERMISOS);

        LocalBroadcastManager.getInstance(this).registerReceiver(mAccionesUI, new IntentFilter("servicio"));

        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout((BeaconParser.EDDYSTONE_UID_LAYOUT)));

        ArrayList<Identifier> identifiers = new ArrayList<>();
        mRegion = new Region(ALL_BEACONS_REGION, identifiers);

        btnComenzarLecturaBeacons = (Button) findViewById(R.id.btnComenzarLecturaBeacons);
        btnDetenerLecturaBeacons = (Button) findViewById(R.id.btnDetenerLecturaBeacons);
        lvBeacons = (ListView) findViewById(R.id.lvBeacons);
        tvDispositivosEncontrados = (TextView) findViewById(R.id.tvDispositivosEncontrados);

        btnComenzarLecturaBeacons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!view.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(MainActivity.this, "No soporta BLE", Toast.LENGTH_SHORT).show();
                }
                else
                {
//                    Toast.makeText(MainActivity.this, "Soporta BLE", Toast.LENGTH_SHORT).show();
                    //mBluetoothManager = (BluetoothManager) view.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    PrepararDeteccion();

//                    //permitira ejecutar la deteccion de beacons solo si el dispositivo es
//                    //totalmente compatible con el protocolo que se busca implementar
//                    if(Build.VERSION.SDK_INT >= 26)
//                    {
//                        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
//                        if(advertiser.equals(null))
//                        {
//                            mostrarMensajeToast("No tiene modulo de Advertising");
//                            return;
//                        }
//                        if(mBluetoothAdapter.isLe2MPhySupported())
//                        {
//                            mostrarMensajeToast("el dispositivo no soporta 2M PHY");
//                            return;
//                        }
//
//                        if(mBluetoothAdapter.isLeExtendedAdvertisingSupported())
//                        {
//                            mostrarMensajeToast("el dispositivo no soporta LE Extended Advertising");
//                            return;
//                        }
//
//                        mostrarMensajeToast("Soporte BLE Extendido");
//                        PrepararDeteccion();
//                    }
//                    else mostrarMensajeToast("no soporta BLE Extendido");

                    //PrepararDeteccion();
                }

//                comenzarDeteccionBeacons();
            }
        });

        btnDetenerLecturaBeacons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetenerDeteccionBeacons();
                // beaconTransmitter.stopAdvertising(); //Detiene la transmision de blotooth low energy
                // Desactivar bluetooth
                if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.disable();
                isBluetoothEnabled = false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InhabilitarBeacons();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccionesUI);
    }

    ///region SOLICITUD DE PERMISOS

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // se revisa el caso de respuesta
        switch(requestCode)
        {
            case 1:
                //si el usuario no garantizo los permisos se actua CERRANDO LA APP
                if (grantResults.length <= 0  && grantResults[0] != PackageManager.PERMISSION_GRANTED) CerrarAplicacion();
                break;
        }
    }

    public static boolean PermisosGarantizados(Context context, String... permisos) {
        if (context != null && permisos != null) {
            for (String permiso : permisos) {
                if (ActivityCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /***
     *  procedimiento que cierra la aplicacion segun la version de compilacion.
     */
    private void CerrarAplicacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) finishAffinity();
        else finish();
    }

    ///endregion

    ///region SONIDO Y VIBRACION DE EQUIPO

    private void soundAndVibrateIfTheDeviceIsLessThanTwoMeters(String distancia) {

        sonidoAlert = MediaPlayer.create(this, R.raw.alert_beep);
        vibracion = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        double dist = Double.parseDouble(distancia);
//        Double distancia = new Double(beacon.getDistancia());
        double distanciaPermitida = (double) 2L;

        int comparable = Double.compare(dist, distanciaPermitida);
        switch (comparable) {
            case -1:
                sonidoAlert.start();
                vibracion.vibrate(DEFAULT_VIBRATE_PERIOD_MS);
                break;
            case 1:
                break;
        }
    }

    ///endregion

    ///region BEACON

    @Override
    public void onBeaconServiceConnect() {
        // Empezar a buscar los beacons que encajen con el el objeto Región pasado, incluyendo
        // actualizaciones en la distancia estimada
        try {
            // Empezar a buscar los beacons que encajen con el el objeto Región pasado, incluyendo
            // actualizaciones en la distancia estimada
//            mBeaconManager.startRangingBeaconsInRegion(mRegion);
            comenzarDeteccionBeacons();
            trasmitirBluetoothLowEnergy();

            mensajeServicio(4, getString(R.string.start_looking_for_beacons));

//            mostrarMensajeToast(getString(R.string.start_looking_for_beacons));

        } catch (Exception e) {
            Log.d(TAG, "Se ha producido una excepción al empezar a buscar beacons " + e.getMessage());
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        try {
            mArrayBeacons = new ArrayList<BeaconControls>();

            if (beacons.size() == 0) {
                mensajeServicio( 1, VACIO);
                mensajeServicio(4, getString(R.string.no_beacons_detected));
//                mostrarMensajeToast(getString(R.string.no_beacons_detected));
            } else {
                for (Beacon beacon : beacons) {
                    BeaconControls beaconControl = new BeaconControls();
                    beaconControl.setBeaconsBTAdd(String.valueOf(beacon.getBluetoothAddress()));
                    beaconControl.setDistancia(String.valueOf(beacon.getDistance()));
                    mArrayBeacons.add(beaconControl);
                    mensajeServicio(5, String.valueOf(beacon.getDistance()));
                }
                mensajeServicio(2, String.valueOf(beacons.size()));
            }
        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: didRangeBeaconsInRegion" + e.getMessage());
            mostrarMensajeToast("Ocurrio una Exception en el metodo: didRangeBeaconsInRegion " + e.getMessage());
        }
    }


    public void trasmitirBluetoothLowEnergy() {
        /*
        Major and Minor values are numbers assigned to your iBeacons, in order to identify them with greater accuracy than using UUID alone.
        Minor and Major are unsigned integer values between 0 and 65535.
        The iBeacon standard requires both a Major and Minor value to be assigned.
         */
        if (beaconTransmitter == null) {
            try {
                String major, minor, uuid;
                // uuid = "94339309-bfe2-4807-b747-9aee23508620"; for test
                uuid = getUUID(getApplicationContext());
                major = "100";

                minor = "1";

                currentType = beaconLayout;
                currentuuid = uuid;
                currentmajorValue = major;
                currentminorValue = minor;

                beacon = new Beacon.Builder()
                        .setId1(uuid)
                        .setId2(major)
                        .setId3(minor)
                        .setManufacturer(0x0118) // It is for AltBeacon.  Change this for other beacon layouts
                        .setTxPower(-59)
                        .setDataFields(Arrays.asList(new Long[]{0l})) // Remove this for beacon layouts without d: fields
                        .build();

                // Change the layout below for other beacon types
                beaconParser = new BeaconParser().setBeaconLayout(parserLayout[beaconLayout]);

                beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);

                beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        mensajeServicio( 4, getString(R.string.beacon_is_transmiting));
//                        mostrarMensajeToast(getString(R.string.beacon_is_transmiting));
                        super.onStartSuccess(settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        mensajeServicio( 4,getString(R.string.beacon_is_stop_transmiting));
//                        mostrarMensajeToast(getString(R.string.beacon_is_stop_transmiting));
                        super.onStartFailure(errorCode);
                    }
                });
                mensajeServicio( 4, getString(R.string.no_beacons_detected));
//                mostrarMensajeToast(getString(R.string.no_beacons_detected));
            } catch (Exception e) {
                Log.d(TAG, "Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy" + e.getMessage());
                mostrarMensajeToast("Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
            }
        }
        else
        {
            try {
                mensajeServicio( 4, getString(R.string.beacons_start_transmision));
//                mostrarMensajeToast(getString(R.string.beacons_start_transmision));
            } catch (Exception e) {
                Log.d(TAG, "Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy" + e.getMessage());
                mensajeServicio(4,"Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
//                mostrarMensajeToast("Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
            }
//            if (isBluetoothEnabled) {
//                try {
//
//                    mostrarMensajeToast(getString(R.string.beacons_start_transmision));
//                } catch (Exception e) {
//                    Log.d(TAG, "Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy" + e.getMessage());
//                    mostrarMensajeToast("Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
//                }
//            } else {
//                // Pedir al usuario que active el bluetooth
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
//                //mostrarMensajeToast(getString(R.string.blutooth_is_not_active));
//            }
        }
    }

    /**
     * Activar localización y bluetooth para empezar a detectar beacons
     */
    private void PrepararDeteccion() {

        try {
            if (mBluetoothAdapter == null)
            {
                mostrarMensajeToast(getString(R.string.not_support_bluetooth_msg));
            }
            else if (mBluetoothAdapter.isEnabled())
            {
                try {
                    mBeaconManager.setBackgroundBetweenScanPeriod(DEFAULT_SCAN_PERIOD_MS);
                    mBeaconManager.setForegroundBetweenScanPeriod(DEFAULT_SCAN_PERIOD_MS);
                    //TODO a partir de esta linea comienzan los errores...
                    mBeaconManager.bind(this);
                    mBeaconManager.addRangeNotifier(this);

                    AlternarComienzarParar(false);
                    mostrarMensajeToast(getString(R.string.inicializing_beacons));
//                    // Desactivar botón de comenzar
//                    btnComenzarLecturaBeacons.setEnabled(true);
//                    btnComenzarLecturaBeacons.setAlpha(1);
//                    mostrarMensajeToast(getString(R.string.inicializing_beacons));
//                    // Activar botón de parar
//                    btnDetenerLecturaBeacons.setEnabled(false);
//                    btnDetenerLecturaBeacons.setAlpha(.5f);

                } catch (Exception e) {
                    Log.d(TAG, "Ocurrio una Exception en el metodo: comenzarDeteccionBeacons" + e.getMessage());
                    mostrarMensajeToast("Ocurrio una Exception en el metodo: comenzarDeteccionBeacons " + e.getMessage());
                }
            }
            else
            {
                // Pedir al usuario que active el bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: PrepararDeteccion" + e.getMessage());
            mostrarMensajeToast("Ocurrio una Exception en el metodo: PrepararDeteccion " + e.getMessage());
        }
    }

    /**
     * Empezar a detectar los beacons, ocultando o mostrando los botones correspondientes
     */
    private void comenzarDeteccionBeacons() {
        try {

            mBeaconManager.startRangingBeaconsInRegion(mRegion);
            mensajeServicio( 3, OPCION_2);
            mensajeServicio(4, getString(R.string.start_looking_for_beacons));

//            // Desactivar botón de comenzar
//            btnComenzarLecturaBeacons.setEnabled(false);
//            btnComenzarLecturaBeacons.setAlpha(.5f);
//            mostrarMensajeToast(getString(R.string.start_looking_for_beacons));
//            // Activar botón de parar
//            btnDetenerLecturaBeacons.setEnabled(true);
//            btnDetenerLecturaBeacons.setAlpha(1);

        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: comenzarDeteccionBeacons" + e.getMessage());

            mensajeServicio( 4, "Ocurrio una Exception en el metodo: comenzarDeteccionBeacons" + e.getMessage());

//            mostrarMensajeToast("Ocurrio una Exception en el metodo: comenzarDeteccionBeacons " + e.getMessage());
        }
    }

    /**
     * Detiene la deteccion de beacons, ocultando o mostrando los botones correspondientes
     */
    private void DetenerDeteccionBeacons() {

        try {
            mBeaconManager.stopMonitoringBeaconsInRegion(mRegion);

            mostrarMensajeToast(getString(R.string.stop_looking_for_beacons));
        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: DetenerDeteccionBeacons" + e.getMessage());
            mostrarMensajeToast("Ocurrio una Exception en el metodo: DetenerDeteccionBeacons " + e.getMessage());
        }

        InhabilitarBeacons();

//        mBeaconManager.removeAllRangeNotifiers();

        //se limpia el ListView
        mensajeServicio( 1, VACIO);

        // Desenlazar servicio de beacons
//        mBeaconManager.unbind(this);
        mensajeServicio(3, OPCION_1);


//        // Activar botón de comenzar
//        btnComenzarLecturaBeacons.setEnabled(true);
//        btnComenzarLecturaBeacons.setAlpha(1);
//
//        // Desactivar botón de parar
//        btnDetenerLecturaBeacons.setEnabled(false);
//        btnDetenerLecturaBeacons.setAlpha(.5f);
    }

    ///endregion

    ///region MANEJO DE INTERFAZ

    /**
     * Mostrar mensaje
     *
     * @param message mensaje a enseñar
     */
    private void mostrarMensajeToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void LimpiarListView() {
        tvDispositivosEncontrados.setText("0");
        mArrayBeacons = new ArrayList<BeaconControls>();
        mBeaconsListAdapter = new BeaconsListAdapter(this, R.layout.list_adapter_beacons, mArrayBeacons);
        lvBeacons.setAdapter(mBeaconsListAdapter);
    }

    /// procedimiento que permite alternar el estado de los botones segun sea necesario
    /// TRUE = Activacion de Comenzar / Desactivacion de Parar
    /// FALSE = Activacion de Parar / Desactivacion de Comenzar
    private void AlternarComienzarParar(boolean opcion)
    {
        if(opcion)
        {
            // ACTIVA botón de comenzar
            btnComenzarLecturaBeacons.setEnabled(opcion);
            btnComenzarLecturaBeacons.setAlpha(1f);
            // DESACTIVA boton parar
            btnDetenerLecturaBeacons.setEnabled(!opcion);
            btnDetenerLecturaBeacons.setAlpha(.5f);
        }
        else
        {
            // ACTIVA botón de parar
            btnDetenerLecturaBeacons.setEnabled(!opcion);
            btnDetenerLecturaBeacons.setAlpha(1f);
            // DESACTIVA botón de comenzar
            btnComenzarLecturaBeacons.setEnabled(opcion);
            btnComenzarLecturaBeacons.setAlpha(.5f);
        }
    }

    private void InhabilitarBeacons()
    {
        mBeaconManager.removeAllRangeNotifiers();
        mBeaconManager.unbind(this);
    }

    ///endregion


    ///region BROADCAST INTERNO

    private void mensajeServicio(int tipo, String mensaje) {
        Intent intent = new Intent("servicio");
        intent.putExtra("tipo", tipo);
        intent.putExtra("mensaje", mensaje);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
    }

    private BroadcastReceiver mAccionesUI = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                int tipo = (int) intent.getExtras().get("tipo");
                String mensaje = (String) intent.getExtras().get("mensaje");
                switch (tipo) {
                    case 1:
                        LimpiarListView();
                        break;
                    case 2:
                        tvDispositivosEncontrados.setText(mensaje);
                        if (mArrayBeacons.size() > 0) {
                            BeaconControls mBeacon = new BeaconControls();
                            ArrayList<BeaconControls> mArrayAuxiliar = new ArrayList<BeaconControls>();
                            for (int indice = 0; indice < mArrayBeacons.size(); indice++) {
                                mBeacon = mArrayBeacons.get(indice);
                                mArrayAuxiliar.add(mBeacon);
                                mBeaconsListAdapter = new BeaconsListAdapter(context, R.layout.list_adapter_beacons, mArrayAuxiliar);
                                lvBeacons.setAdapter(mBeaconsListAdapter);
                            }
                        }
                        break;
                    case 3:
                        if(mensaje.equals(OPCION_1)) AlternarComienzarParar(true);
                        else AlternarComienzarParar(false);
                        break;
                    case 4:
                        mostrarMensajeToast(mensaje);
                        break;
                    case 5:
                        soundAndVibrateIfTheDeviceIsLessThanTwoMeters(mensaje);
                        break;
                }

            } catch (Exception e) {
            }
        }
    };

    ///endregion

}