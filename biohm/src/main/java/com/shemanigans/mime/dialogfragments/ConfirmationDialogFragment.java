package com.shemanigans.mime.dialogfragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseDialogFragment;

@SuppressLint("InflateParams")
public class ConfirmationDialogFragment extends BaseDialogFragment {

	public boolean selection = false;
	ProgressBar stateBar;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Set the dialog title
		builder.setTitle(R.string.writer);
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_confirmation, null);
		stateBar = (ProgressBar) view.findViewById(R.id.state_bar);
		stateBar.getIndeterminateDrawable().setColorFilter(
				getResources().getColor(R.color.primary),
				android.graphics.PorterDuff.Mode.SRC_IN);
		builder.setView(view);
		return builder.create();
	}

	// Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	public boolean getSelection() {
		return this.selection;
	}
}
