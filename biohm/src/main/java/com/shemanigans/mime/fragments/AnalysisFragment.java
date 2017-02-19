package com.shemanigans.mime.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BroadcastReceiverFragment;
import com.shemanigans.mime.models.RXpair;
import com.shemanigans.mime.models.Series;
import com.shemanigans.mime.models.TaubinSolution;
import com.shemanigans.mime.services.BluetoothLeService;

import java.util.Locale;

public class AnalysisFragment extends BroadcastReceiverFragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    private Series.RXSeries measuredSeries = new Series.RXSeries();
    private Series.RXSeries fittedSeries = new Series.RXSeries();

    // Declare UI references
    private TextView reTextView, riTextView, cTextView, RMSETextView, rsqTextView, alphaTextView, fPeakTextView;
    private TextView fragmentTitle;
    private XYPlot fluidStatusPlot = null;
    private TableRow heartRateHeader, heartRateValues, fluidStatusHeader, fluidStatusValues, statsHeader, statsValues;

    private RXpair minRX = new RXpair(0, 0);
    private RXpair maxRX = new RXpair(5000, 5000);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AnalysisFragment newInstance(int sectionNumber) {
        AnalysisFragment fragment = new AnalysisFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public AnalysisFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_analysis, container, false);

        fluidStatusPlot = (XYPlot) rootView.findViewById(R.id.fluidStatusPlot);
        fragmentTitle = (TextView) rootView.findViewById(R.id.fragment_title);

        reTextView = (TextView) rootView.findViewById(R.id.re);
        riTextView = (TextView) rootView.findViewById(R.id.ri);
        cTextView = (TextView) rootView.findViewById(R.id.c);

        alphaTextView = (TextView) rootView.findViewById(R.id.alpha);
        fPeakTextView = (TextView) rootView.findViewById(R.id.f_peak);
        RMSETextView = (TextView) rootView.findViewById(R.id.rmse);
        rsqTextView = (TextView) rootView.findViewById(R.id.rsq);

        heartRateHeader = (TableRow) rootView.findViewById(R.id.heart_rate_headers);
        heartRateValues = (TableRow) rootView.findViewById(R.id.heart_rate_values);
        fluidStatusHeader = (TableRow) rootView.findViewById(R.id.fluid_status_headers);
        fluidStatusValues = (TableRow) rootView.findViewById(R.id.fluid_status_values);

        statsHeader = (TableRow) rootView.findViewById(R.id.stats_headers);
        statsValues = (TableRow) rootView.findViewById(R.id.stats_values);

        // 	Create a couple arrays of y-values to plot:
        setupPlot();
        setup();
        return rootView;
    }

    public void onTaubinSolution(TaubinSolution taubinSolution) {
        if (getView() != null) {
            setFluidStatusParams(taubinSolution);
            updatePlot(taubinSolution);
        }
    }

    public void setup() {

        boolean freqSweepOn = BluetoothLeService.deviceStatus.freqSweepOn;

        fragmentTitle.setText(freqSweepOn ? R.string.fluid_status : R.string.heart_rate);

        int ying = freqSweepOn ? View.GONE : View.VISIBLE;
        int yang = freqSweepOn ? View.VISIBLE : View.GONE;

        heartRateHeader.setVisibility(ying);
        heartRateValues.setVisibility(ying);

        fluidStatusHeader.setVisibility(yang);
        fluidStatusValues.setVisibility(yang);
        fluidStatusPlot.setVisibility(yang);
        statsHeader.setVisibility(yang);
        statsValues.setVisibility(yang);
    }

    public void setFluidStatusParams(TaubinSolution taubinSolution) {
        reTextView.setText(String.format(Locale.US, "%.1f", taubinSolution.re));
        riTextView.setText(String.format(Locale.US, "%.1f", taubinSolution.ri));
        cTextView.setText(String.format(Locale.US, "%.2e", taubinSolution.c));
        RMSETextView.setText(String.format(Locale.US, "%.1f", taubinSolution.RMSE));
        rsqTextView.setText(String.format(Locale.US, "%.2f", taubinSolution.RSQ));
        alphaTextView.setText(String.format(Locale.US, "%.3f", taubinSolution.alpha));
        fPeakTextView.setText(String.format(Locale.US, "%.2f", taubinSolution.fPeak));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fluidStatusPlot = null;
        fragmentTitle = null;
        reTextView = null;
        riTextView = null;
        cTextView = null;
        alphaTextView = null;
        fPeakTextView = null;
        RMSETextView = null;
        rsqTextView = null;
        heartRateHeader = null;
        heartRateValues = null;
        fluidStatusHeader = null;
        fluidStatusValues = null;
        statsHeader = null;
        statsValues = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BluetoothLeService.DATA_AVAILABLE_FREQUENCY_PARAMS:
                setup();
                break;
            case BluetoothLeService.DATA_AVAILABLE_TAUBIN_SOLUTION:
                TaubinSolution taubinSolution = intent.getParcelableExtra(BluetoothLeService.DATA_AVAILABLE_TAUBIN_SOLUTION);
                onTaubinSolution(taubinSolution);
                break;
        }
    }

    public void updatePlot(TaubinSolution taubinSolution) {
        measuredSeries.updateRxPairs(taubinSolution.measuredRXPairs);
        fittedSeries.updateRxPairs(taubinSolution.fittedRXPairs);
        fluidStatusPlot.redraw();
    }

    private void setupPlot() {
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#fffafafa"));
        bgPaint.setStyle(Paint.Style.FILL);

        fluidStatusPlot.setRangeBoundaries(minRX.resistance, maxRX.reactance, BoundaryMode.AUTO);
        fluidStatusPlot.setDomainBoundaries(minRX.resistance, maxRX.reactance, BoundaryMode.AUTO);

        // Format general area
        fluidStatusPlot.setBackgroundColor(Color.WHITE);
        fluidStatusPlot.getBackgroundPaint().set(bgPaint);
        fluidStatusPlot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
        fluidStatusPlot.getGraph().setGridBackgroundPaint(null);
        fluidStatusPlot.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
        fluidStatusPlot.getGraph().setPadding(12, 12, 12, 12);
        fluidStatusPlot.setPlotMargins(0, 0, 0, 0);
        fluidStatusPlot.getTitle().setText("");

        fluidStatusPlot.setBorderPaint(bgPaint);
        //fluidStatusPlot.getGraph().getBorderPaint().setColor(Color.TRANSPARENT);

        // Format domain
        fluidStatusPlot.getDomainTitle().getLabelPaint().setColor(Color.parseColor("#006bb2"));
        fluidStatusPlot.getDomainTitle().getLabelPaint().setTextSize(20);
        // fluidStatusPlot.getGraph().getDomainTickLabelPaint().setTextSize(20);
        // fluidStatusPlot.getGraph().getDomainTickLabelPaint().setColor(Color.BLACK);
        fluidStatusPlot.getGraph().getDomainOriginLinePaint().setColor(Color.BLACK);
        fluidStatusPlot.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        fluidStatusPlot.getGraph().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);
        fluidStatusPlot.setDomainStep(StepMode.SUBDIVIDE, 10);
        //fluidStatusPlot.setDomainStepValue(50);
        //fluidStatusPlot.setTicksPerDomainLabel(50);
        fluidStatusPlot.setDomainLabel("Resistance");
        //fluidStatusPlot.getDomainLabel().pack();
        //fluidStatusPlot.setDomainValueFormat(new DecimalFormat("0"));

        // Format range
        //fluidStatusPlot.getRangeLabel().getLabelPaint().setColor(Color.parseColor("#006bb2"));
        //fluidStatusPlot.getRangeLabel().getLabelPaint().setTextSize(20);
        //fluidStatusPlot.getGraph().getRangeOriginTickLabelPaint().setTextSize(20);
        fluidStatusPlot.getGraph().getRangeOriginLinePaint().setColor(Color.BLACK);
        //fluidStatusPlot.getGraph().getRangeOriginTickLabelPaint().setColor(Color.BLACK);
        fluidStatusPlot.getGraph().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        fluidStatusPlot.getGraph().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        fluidStatusPlot.setRangeStep(StepMode.SUBDIVIDE, 10);
        //fluidStatusPlot.setRangeStepValue(50);
        //fluidStatusPlot.setTicksPerRangeLabel(50);
        fluidStatusPlot.setRangeLabel("Reactance");
        //fluidStatusPlot.getRangeLabel().pack();
        //fluidStatusPlot.setRangeValueFormat(new DecimalFormat("0"));

        // Format legend

        fluidStatusPlot.getLegend().getTextPaint().setColor(Color.parseColor("#006bb2"));
        fluidStatusPlot.getLegend().getTextPaint().setTextSize(20);
        fluidStatusPlot.getLegend().setPaddingBottom(10);

        // Add series
        fluidStatusPlot.addSeries(measuredSeries, new LineAndPointFormatter(Color.parseColor("#008b8b"), Color.BLUE, null, null));
        fluidStatusPlot.addSeries(fittedSeries, new LineAndPointFormatter(Color.parseColor("#8b008b"), Color.RED, null, null));
        fluidStatusPlot.getBackgroundPaint().set(bgPaint);
    }
}
