package com.shemanigans.mime;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shemanigans.mime.abstractclasses.BaseFragment;

public class NoFragment extends BaseFragment {

    public static NoFragment newInstance() {
        NoFragment fragment = new NoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_no, container, false);
    }
}
