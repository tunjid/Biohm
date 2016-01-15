package com.shemanigans.mime.abstractclasses;

import android.support.v4.app.Fragment;
import android.view.View;

import com.shemanigans.mime.services.BluetoothLeService;

/**
 * Base fragment
 */
public abstract class BaseFragment extends Fragment{

    public int showView(boolean show) {
        return show
                ? View.VISIBLE
                :View.GONE;
    }
    public BluetoothLeService getBleService() {
        return ((BaseActivity)getActivity()).getBleService();
    }

}
