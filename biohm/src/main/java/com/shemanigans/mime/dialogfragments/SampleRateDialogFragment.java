package com.shemanigans.mime.dialogfragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseDialogFragment;
import com.shemanigans.mime.fragments.OverviewParentFragment;

@SuppressLint("InflateParams")
public class SampleRateDialogFragment extends BaseDialogFragment {

    private static final String SAMPLE_RATE = "SAMPLE_RATE";

    private byte sampleRate = 0;

    // Use this instance of the interface to deliver action events
    SampleRateListener mListener;

    public static SampleRateDialogFragment newInstance(byte sampleRate) {

        SampleRateDialogFragment sampleRateDialogFragment = new SampleRateDialogFragment();
        Bundle args = new Bundle();
        args.putByte(SAMPLE_RATE, sampleRate);
        sampleRateDialogFragment.setArguments(args);
        return sampleRateDialogFragment;
    }

    public SampleRateDialogFragment() {

    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getParentFragment() != null) {
            mListener = ((OverviewParentFragment) getParentFragment());
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.set_sample_rate, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.set_sample_rate);
        final TextView sampleRatetext = (TextView) view.findViewById(R.id.current_frequency);


        builder.setTitle(R.string.set_sample_rate);

        sampleRate = getArguments().getByte(SAMPLE_RATE);

        seekBar.setProgress(((int)sampleRate / 5) - 1);
        sampleRatetext.setText(Integer.toString(sampleRate));

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                sampleRate = (byte)((progressValue + 1) * 5);
                sampleRatetext.setText("" + sampleRate); // Make values go from 5 to 90 in increments of 5
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClickSampleRate((byte) sampleRate);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }

    public interface SampleRateListener {
        void onDialogPositiveClickSampleRate(byte sampleRate);
    }
}
