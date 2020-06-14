package ar.com.cdt.socialdistance.ui.fragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import ar.com.cdt.socialdistance.ui.bluetooth.BluetoothDistanceFragment;
import ar.com.cdt.socialdistance.ui.geolocation.LocationFragment;

/**
 * Created by Marcos Lozina on 19/05/2020. Pandemia Edition
 */
public class SimpleFragmentPageAdapter extends FragmentPagerAdapter {

    private String tabsTitles[] = new String[] {""};

    public SimpleFragmentPageAdapter (FragmentManager fm)
    {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        if (position == 0) return new BluetoothDistanceFragment();
        else  return new LocationFragment();
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabsTitles[position];
    }
}
