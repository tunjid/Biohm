package com.shemanigans.mime.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseFragment;


public class StartFragment extends BaseFragment
        implements View.OnClickListener {

    public static StartFragment newInstance() {
        StartFragment startFragment = new StartFragment();
        Bundle args = new Bundle();
        startFragment.setArguments(args);
        return startFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_start, container, false);
        View yesButton = rootView.findViewById(R.id.yes);
        View noButton = rootView.findViewById(R.id.no);

        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.yes:
                showFragment(PairFragment.newInstance());
                break;
            case R.id.no:
                showFragment(NoFragment.newInstance());
                break;
        }
    }
}
