package com.shemanigans.mime.dialogfragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseDialogFragment;

@SuppressLint("InflateParams")
public class ConfirmationDialogFragment extends BaseDialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.writer)
                .setView(inflater.inflate(R.layout.fragment_confirmation, null))
                .create();
    }
}
