package ar.com.cdt.socialdistance.ui.controls;

import java.math.BigDecimal;

/**
 * Created by Marcos Lozina on 18/05/2020. Pandemia Edition
 */

public class BeaconControls {
    private String mBeaconsBTAdd, mDistancia;
    private static String DISTANCIA_FIJA = "0,1";

    public BeaconControls(){}
    public BeaconControls(String cantidadDispositivos, String beaconsBTAdd, String distancia)
    {
        mBeaconsBTAdd = beaconsBTAdd;
        mDistancia = distancia;
    }

    public String getBeaconsBTAdd() {return mBeaconsBTAdd; }
    public String getDistancia() {return mDistancia; }

    public void setBeaconsBTAdd(String beaconsBTAdd) {mBeaconsBTAdd = beaconsBTAdd; }


    public void setDistancia(String distancia) {

        /*  Como requemiento se pidio aplicar la siguiente funcion sobre los datos de distancia:

            1. Si la distancia es menor  a 3 metros, la distancia seran los decimales que viene en la medida.
            2. Si la distancia es igual  a 3 metros, la distancia sera 0,1.
            3. Si la distancia es mayor a 3 metros, la distancia sera igual a (distancia - 3).
         */

        double distanciaSinCorreccion = Double.parseDouble(distancia); //distancia recibida
        double sesgo = (double) 3L; //3 metros

        //d1 se compara con d2
        int comparable = Double.compare(distanciaSinCorreccion, sesgo);

        //d1 = d2 -- la distancia es igual a 3 mts
        //d1 7< d2 -- la distancia es menor a 3 mts
        //d1 > d2 -- la distancia es mayor a 3 mts
        if(comparable == 0) mDistancia = DISTANCIA_FIJA;
        else if (comparable < 0) mDistancia = String.valueOf(distanciaSinCorreccion);
        else
        {
            double distanciaConCorreccion = distanciaSinCorreccion - sesgo;
            mDistancia = String.valueOf(distanciaConCorreccion);
        }

//        switch (comparable){
//            case -1: //d1 < d2
//                BigDecimal distanciaFormatoBigDecimal = new BigDecimal(distancia);
//                mDistancia= distanciaFormatoBigDecimal.remainder(BigDecimal.ONE).toString();
//                break;
//            case 0: //d1 = d2
//                mDistancia= "0,1";
//                break;
//            case 1: //d1 > d2
//                distanciaConCorreccion= distanciaSinCorreccion - sesgo;
//                mDistancia = distanciaConCorreccion.toString();
//                break;
//        }

//        mDistancia = distancia;
    }
}
