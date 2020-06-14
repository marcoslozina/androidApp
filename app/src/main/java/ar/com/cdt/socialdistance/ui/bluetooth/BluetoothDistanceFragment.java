package ar.com.cdt.socialdistance.ui.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import ar.com.cdt.socialdistance.MainActivity;
import ar.com.cdt.socialdistance.R;
import ar.com.cdt.socialdistance.bluetoothHelper.BluetoothHelper;
import ar.com.cdt.socialdistance.bluetoothHelper.BluetoothListener;
import ar.com.cdt.socialdistance.bluetoothHelper.BluetoothListener.BluetoothTrigger;
import ar.com.cdt.socialdistance.bluetoothHelper.BluetoothListener.OnBluetoothSupportedCheckListener;
import ar.com.cdt.socialdistance.ui.adapters.BeaconsListAdapter;
import ar.com.cdt.socialdistance.ui.controls.BeaconControls;

import static ar.com.cdt.socialdistance.bluetoothHelper.BluetoothListener.*;

/**
 * Created by Marcos Lozina on 19/05/2020. Pandemia Edition
 */

public class BluetoothDistanceFragment extends Fragment implements BeaconConsumer, RangeNotifier, OnBluetoothSupportedCheckListener, OnBluetoothEnabledCheckListener,
        BluetoothTrigger {

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

    // Para interactuar con los beacons desde una actividad
    private BeaconManager mBeaconManager;

    // Representa el criterio de campos con los que buscar beacons
    private Region mRegion;

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



    public BluetoothDistanceFragment() {
    }

    public synchronized static String getUUID(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_bluetooth_distance, container, false);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccionesUI, new IntentFilter("servicio"));

        mBeaconManager = BeaconManager.getInstanceForApplication(getContext());
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout((BeaconParser.EDDYSTONE_UID_LAYOUT)));

        ArrayList<Identifier> identifiers = new ArrayList<>();
        mRegion = new Region(ALL_BEACONS_REGION, identifiers);

        btnComenzarLecturaBeacons = (Button) root.findViewById(R.id.btnComenzarLecturaBeacons);
        btnDetenerLecturaBeacons = (Button) root.findViewById(R.id.btnDetenerLecturaBeacons);
        lvBeacons = (ListView) root.findViewById(R.id.lvBeacons);
        tvDispositivosEncontrados = (TextView) root.findViewById(R.id.tvDispositivosEncontrados);

        btnComenzarLecturaBeacons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                comenzarDeteccionBeacons();
            }
        });

        btnDetenerLecturaBeacons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetenerDeteccionBeacons();
               // beaconTransmitter.stopAdvertising(); //Detiene la transmision de blotooth low energy
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                // Desactivar bluetooth
                if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.disable();
                isBluetoothEnabled = false;
            }
        });

        this.PrepararDeteccion();
        return root;
    }

    @Override
    public void onBeaconServiceConnect() {
        // Empezar a buscar los beacons que encajen con el el objeto Región pasado, incluyendo
        // actualizaciones en la distancia estimada
        trasmitirSeñalesBluetoothLowEnergy();


    }

    /////////////////////////////transmisor/////////////////////////////////////////////////////

    public void trasmitirSeñalesBluetoothLowEnergy() {

        if (beaconTransmitter == null) {
/*
Major and Minor values are numbers assigned to your iBeacons, in order to identify them with greater accuracy than using UUID alone.
Minor and Major are unsigned integer values between 0 and 65535.
The iBeacon standard requires both a Major and Minor value to be assigned.
 */
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
                        mostrarMensajeToast(getString(R.string.beacon_is_transmiting));
                        super.onStartSuccess(settingsInEffect);

                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        mostrarMensajeToast(getString(R.string.beacon_is_stop_transmiting));

                        super.onStartFailure(errorCode);
                    }
                });
                mostrarMensajeToast(getString(R.string.no_beacons_detected));
            } catch (Exception e) {
                Log.d(TAG, "Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy" + e.getMessage());
                mostrarMensajeToast("Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
            }

        } else {


            if (isBluetoothEnabled) {
                try {

                    mostrarMensajeToast(getString(R.string.beacons_start_transmision));
                } catch (Exception e) {
                    Log.d(TAG, "Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy" + e.getMessage());
                    mostrarMensajeToast("Ocurrio una Exception en el metodo: trasmitirSeñalesBluetoothLowEnergy " + e.getMessage());
                }

            } else {
                // Pedir al usuario que active el bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                //mostrarMensajeToast(getString(R.string.blutooth_is_not_active));
            }


        }

    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        try {
            mArrayBeacons = new ArrayList<BeaconControls>();

            if (beacons.size() == 0) {
                mensajeServicio(getContext(), 1, "");
                mostrarMensajeToast(getString(R.string.no_beacons_detected));
            } else {
                for (Beacon beacon : beacons) {
                    BeaconControls beaconControl = new BeaconControls();

                    beaconControl.setBeaconsBTAdd(String.valueOf(beacon.getBluetoothAddress()));
                    beaconControl.setDistancia(String.format("%.2f", beacon.getDistance()));
                    mArrayBeacons.add(beaconControl);
                    soundAndVibrateIfTheDeviceIsLessThanTwoMeters(beaconControl);
                }

                mensajeServicio(getContext(), 2, String.valueOf(beacons.size()));
            }
        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: didRangeBeaconsInRegion" + e.getMessage());
            mostrarMensajeToast("Ocurrio una Exception en el metodo: didRangeBeaconsInRegion " + e.getMessage());
        }
    }

    private void soundAndVibrateIfTheDeviceIsLessThanTwoMeters(BeaconControls beacon) {

        sonidoAlert = MediaPlayer.create(getContext(), R.raw.alert_beep);
        vibracion = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        Double distancia = new Double(beacon.getDistancia());
        Double distanciaPermitida = new Double(2L);

        Integer comparable = Double.compare(distancia, distanciaPermitida);
        switch (comparable) {
            case -1:
                sonidoAlert.start();
                vibracion.vibrate(DEFAULT_VIBRATE_PERIOD_MS);
                break;
            case 1:
                break;
        }
    }

    @Override
    public Context getApplicationContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        getActivity().unbindService(serviceConnection);

    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return getActivity().bindService(intent, serviceConnection, i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBeaconManager.removeAllRangeNotifiers();
        mBeaconManager.unbind(this);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mAccionesUI);
    }

    /**
     * Mostrar mensaje
     *
     * @param message mensaje a enseñar
     */
    private void mostrarMensajeToast(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void LimpiarListView() {
        tvDispositivosEncontrados.setText("0");
        mArrayBeacons = new ArrayList<BeaconControls>();
        //mArrayBeacons.clear();
        mBeaconsListAdapter = new BeaconsListAdapter(getContext(), R.layout.list_adapter_beacons, mArrayBeacons);
        lvBeacons.setAdapter(mBeaconsListAdapter);
    }


    /**
     * Activar localización y bluetooth para empezar a detectar beacons
     */
    private void PrepararDeteccion() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (mBluetoothAdapter == null) {

                mostrarMensajeToast(getString(R.string.not_support_bluetooth_msg));


            } else if (mBluetoothAdapter.isEnabled()) {

                try {
                    bluetoothHelper = new BluetoothHelper();
                    bluetoothHelper.initializeBluetooth(this);
                    mBeaconManager.setBackgroundBetweenScanPeriod(DEFAULT_SCAN_PERIOD_MS);
                    mBeaconManager.setForegroundBetweenScanPeriod(DEFAULT_SCAN_PERIOD_MS);
                    mBeaconManager.bind(this);
                    mBeaconManager.addRangeNotifier(this);

                    // Desactivar botón de comenzar
                   btnComenzarLecturaBeacons.setEnabled(true);
                    btnComenzarLecturaBeacons.setAlpha(1);
                    mostrarMensajeToast(getString(R.string.inicializing_beacons));
                    // Activar botón de parar
                    btnDetenerLecturaBeacons.setEnabled(false);
                    btnDetenerLecturaBeacons.setAlpha(.5f);

                } catch (Exception e) {
                    Log.d(TAG, "Ocurrio una Exception en el metodo: comenzarDeteccionBeacons" + e.getMessage());
                    mostrarMensajeToast("Ocurrio una Exception en el metodo: comenzarDeteccionBeacons " + e.getMessage());
                }

            } else {

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
            // Desactivar botón de comenzar
            btnComenzarLecturaBeacons.setEnabled(false);
            btnComenzarLecturaBeacons.setAlpha(.5f);
            mostrarMensajeToast(getString(R.string.start_looking_for_beacons));
            // Activar botón de parar
            btnDetenerLecturaBeacons.setEnabled(true);
            btnDetenerLecturaBeacons.setAlpha(1);

        } catch (Exception e) {
            Log.d(TAG, "Ocurrio una Exception en el metodo: comenzarDeteccionBeacons" + e.getMessage());
            mostrarMensajeToast("Ocurrio una Exception en el metodo: comenzarDeteccionBeacons " + e.getMessage());
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

        mBeaconManager.removeAllRangeNotifiers();

        //se limpia el ListView
        mensajeServicio(getContext(), 1, "");

        // Desenlazar servicio de beacons
        mBeaconManager.unbind(this);

        // Activar botón de comenzar
        btnComenzarLecturaBeacons.setEnabled(true);
        btnComenzarLecturaBeacons.setAlpha(1);

        // Desactivar botón de parar
        btnDetenerLecturaBeacons.setEnabled(false);
        btnDetenerLecturaBeacons.setAlpha(.5f);
    }

    private static void mensajeServicio(Context context, int tipo, String mensaje) {
        Intent intent = new Intent("servicio");
        intent.putExtra("tipo", tipo);
        intent.putExtra("mensaje", mensaje);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
                                mBeaconsListAdapter = new BeaconsListAdapter(getContext(), R.layout.list_adapter_beacons, mArrayAuxiliar);
                                lvBeacons.setAdapter(mBeaconsListAdapter);
                            }
                        }
                        break;
                }

            } catch (Exception e) {
            }
        }
    };

    @Override
    public void onBLENotSupported() {
        mostrarMensajeToast(getString(R.string.not_support_ble_msg));
    }

    @Override
    public void onBluetoothNotSupported() {
        mostrarMensajeToast(getString(R.string.not_support_bluetooth_msg));
    }

    @Override
    public void onBluetoothEnabled(boolean enable) {
        if (enable) {
            isBluetoothEnabled = true;
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    @Override
    public void initBluetooth() {
        if (bluetoothHelper != null)
            bluetoothHelper.initializeBluetooth(this);
    }

    @Override
    public void enableBluetooth() {
        if (bluetoothHelper != null)
            bluetoothHelper.enableBluetooth(this);
    }
}
