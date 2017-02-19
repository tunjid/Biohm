package com.shemanigans.mime.dialogfragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.shemanigans.mime.R;

@SuppressLint("InflateParams")
public class NameTextFileFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.set_text_file_name, null);

        final EditText editText = (EditText) view.findViewById(R.id.textfile_name);

        //final NameTextFileListener listener = (OverviewParentFragment)getParentFragment();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.name_text_file)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //listener.onDialogPositiveClickNameTextFile(editText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })
                .create();
    }

    public interface NameTextFileListener {
        void onDialogPositiveClickNameTextFile(String fileName);
    }


}
