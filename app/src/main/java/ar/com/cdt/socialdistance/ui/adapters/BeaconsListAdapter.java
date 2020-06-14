package ar.com.cdt.socialdistance.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import ar.com.cdt.socialdistance.R;
import ar.com.cdt.socialdistance.ui.controls.BeaconControls;

/**
 * Created by Marcos Lozina on 19/05/2020. Pandemia Edition
 */

public class BeaconsListAdapter extends ArrayAdapter<BeaconControls> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<BeaconControls> mDatosBeacons;
    private int mRevisarIdRecurso;

    public BeaconsListAdapter(Context context, int tvIDRecurso, ArrayList<BeaconControls> mmDatosBeacons)
    {
        super(context, tvIDRecurso, mmDatosBeacons);
        this.mDatosBeacons = mmDatosBeacons;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRevisarIdRecurso = tvIDRecurso;
    }

    public View getView(int posicion, View convertView, ViewGroup parent)
    {
        convertView = mLayoutInflater.inflate(mRevisarIdRecurso, null);
        BeaconControls mBeacon = mDatosBeacons.get(posicion);

        TextView beaconBTAddress = (TextView) convertView.findViewById(R.id.tvDireccionBeaconBT);
        TextView distancia = (TextView) convertView.findViewById(R.id.tvDistancia);

        beaconBTAddress.setText(mBeacon.getBeaconsBTAdd());
        distancia.setText(mBeacon.getDistancia());

        return convertView;
    }
}
