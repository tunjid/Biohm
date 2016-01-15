package com.shemanigans.mime.abstractclasses;

import android.support.v4.app.DialogFragment;

import com.shemanigans.mime.services.BluetoothLeService;

/**
 * Base fragment
 */
public abstract class BaseDialogFragment extends DialogFragment {

    public BluetoothLeService getBleService() {
        return ((BaseActivity) getActivity()).getBleService();
    }

}
