package com.shemanigans.mime.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BroadcastReceiverFragment;
import com.shemanigans.mime.models.DeviceData;
import com.shemanigans.mime.models.Series;
import com.shemanigans.mime.services.BluetoothLeService;

public class ActivityFragment extends BroadcastReceiverFragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    private Series.GenericSeries zSeries = new Series.GenericSeries("Z");
    private Series.GenericSeries ySeries = new Series.GenericSeries("Y");
    private Series.GenericSeries xSeries = new Series.GenericSeries("X");

    // Declare UI references
    private XYPlot activityPlot = null;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ActivityFragment newInstance(int sectionNumber) {
        ActivityFragment fragment = new ActivityFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_activity, container, false);
        activityPlot = (XYPlot) rootView.findViewById(R.id.activityPlot);

        setupPlot();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        activityPlot = null;
    }

    public void updatePlot(DeviceData deviceData) {
        zSeries.addData(deviceData.accelZ);
        ySeries.addData(deviceData.accelY);
        xSeries.addData(deviceData.accelX);

        // redraw the Plots:
        activityPlot.redraw();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isVisible()) return;

        switch (intent.getAction()) {
            case BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE:
                DeviceData deviceData = intent.getParcelableExtra(BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE);
                updatePlot(deviceData);
                break;
        }
    }

    private void setupPlot() {
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#fffafafa"));
        bgPaint.setStyle(Paint.Style.FILL);

        activityPlot.setRangeBoundaries(-120, 120, BoundaryMode.FIXED);
        activityPlot.setDomainBoundaries(0, 90, BoundaryMode.FIXED);

        // Format general area
        activityPlot.setBackgroundColor(Color.WHITE);
        activityPlot.getBackgroundPaint().set(bgPaint);
        //	activityPlot.getBackgroundPaint().setColor(Color.parseColor("#d8d8d8"));
        activityPlot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
        activityPlot.getGraph().setGridBackgroundPaint(null);
        activityPlot.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
        activityPlot.getGraph().setPadding(15, 15, 15, 15);
        activityPlot.setPlotMargins(0, 0, 0, 0);
        activityPlot.getTitle().setText("");

        activityPlot.setBorderPaint(bgPaint);
        //activityPlot.getGraph().getBorderPaint().setColor(Color.TRANSPARENT);

        // Format domain
//        activityPlot.getDomainLabelWidget().getLabelPaint().setColor(Color.parseColor("#006bb2"));
//        activityPlot.getDomainLabelWidget().getLabelPaint().setTextSize(20);
//        activityPlot.getGraph().getDomainTickLabelPaint().setColor(Color.BLACK);
//        activityPlot.getGraph().getDomainTickLabelPaint().setTextSize(20);
//        activityPlot.getGraph().getDomainOriginTickLabelPaint().setColor(Color.BLACK);
        activityPlot.getGraph().getDomainOriginLinePaint().setColor(Color.BLACK);
        activityPlot.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        activityPlot.setDomainStepValue(5);
        activityPlot.setDomainLabel("Sample Index");
//        activityPlot.getDomainLabelWidget().pack();


        // Format range
//        activityPlot.getRangeLabelWidget().getLabelPaint().setColor(Color.parseColor("#006bb2"));
//        activityPlot.getRangeLabelWidget().getLabelPaint().setTextSize(20);
//        activityPlot.getGraph().getRangeTickLabelPaint().setColor(Color.BLACK);
//        activityPlot.getGraph().getRangeTickLabelPaint().setTextSize(20);
//        activityPlot.getGraph().getRangeOriginTickLabelPaint().setColor(Color.BLACK);
        activityPlot.getGraph().getRangeOriginLinePaint().setColor(Color.BLACK);
        activityPlot.getGraph().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        activityPlot.getGraph().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        activityPlot.setRangeLabel("Data");
//        activityPlot.setTicksPerRangeLabel(3);
//        activityPlot.getRangeLabelWidget().pack();

        // Format legend

        activityPlot.getLegend().getTextPaint().setColor(Color.parseColor("#006bb2"));
        activityPlot.getLegend().getTextPaint().setTextSize(20);
        activityPlot.getLegend().setPaddingBottom(10);

        // Add series

        activityPlot.addSeries(zSeries, new LineAndPointFormatter(Color.parseColor("#006bb2"), null, null, null));
        activityPlot.addSeries(ySeries, new LineAndPointFormatter(Color.parseColor("#008b8b"), null, null, null));
        activityPlot.addSeries(xSeries, new LineAndPointFormatter(Color.parseColor("#8b008b"), null, null, null));

        activityPlot.getBackgroundPaint().set(bgPaint);
    }

}
