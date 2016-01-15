package com.shemanigans.mime.dialogfragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseDialogFragment;
import com.shemanigans.mime.fragments.OverviewParentFragment;

@SuppressLint("InflateParams")
public class FrequencySweepDialogFragment extends BaseDialogFragment {

    private static final String START_FREQUENCY = "START_FREQUENCY";
    private static final String STEP_SIZE = "STEP_SIZE";
    private static final String NUM_INCREMENTS = "NUM_INCREMENTS";

    public static FrequencySweepDialogFragment newInstance(byte startFreq, byte stepSize, byte numIncrements) {

        FrequencySweepDialogFragment frequencySweepDialogFragment = new FrequencySweepDialogFragment();
        Bundle args = new Bundle();

        args.putByte(START_FREQUENCY, startFreq);
        args.putByte(STEP_SIZE, stepSize);
        args.putByte(NUM_INCREMENTS, numIncrements);

        frequencySweepDialogFragment.setArguments(args);
        return frequencySweepDialogFragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getParentFragment() != null) {
            mListener = ((OverviewParentFragment) getParentFragment());
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.set_frequency_sweep, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText startFreqEditText = (EditText) view.findViewById(R.id.set_upper_freq);
        final EditText stepSizeEditText = (EditText) view.findViewById(R.id.set_step_size);
        final EditText numOfIncrementsEditText = (EditText) view.findViewById(R.id.set_lower_freq);

        // Set the dialog title
        builder.setTitle(R.string.set_frequency_sweep);
        // Get the layout inflater

        byte startFreq = getArguments().getByte(START_FREQUENCY);
        byte stepSize = getArguments().getByte(STEP_SIZE);
        byte numIncrements = getArguments().getByte(NUM_INCREMENTS);

        startFreqEditText.setText("" + startFreq);
        stepSizeEditText.setText("" + stepSize);
        numOfIncrementsEditText.setText("" + numIncrements);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.multi_freq, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Get new value for sample rate

                        byte startFreq = Byte.valueOf(startFreqEditText.getText().toString());
                        byte stepSize = Byte.valueOf(stepSizeEditText.getText().toString());
                        byte numIncrements = Byte.valueOf(numOfIncrementsEditText.getText().toString());

                        mListener.onDialogPositiveClickFrequencySweep(startFreq, stepSize, numIncrements);
                        dismiss();
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNeutralClickFrequencySweep();
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.single_freq, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        byte startFreq = Byte.valueOf(startFreqEditText.getText().toString());

                        mListener.onDialogNegativeClickFrequencySweep(startFreq);
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

    public interface FrequencySweepListener {
        void onDialogPositiveClickFrequencySweep(byte startFreq, byte stepSize, byte numIncrements);

        void onDialogNegativeClickFrequencySweep(byte startFreq);

        void onDialogNeutralClickFrequencySweep();
    }

    // Use this instance of the interface to deliver action events
    FrequencySweepListener mListener;

}
