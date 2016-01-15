package com.shemanigans.mime.fragments;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.shemanigans.mime.R;
import com.shemanigans.mime.SampleGattAttributes;
import com.shemanigans.mime.abstractclasses.BaseFragment;
import com.shemanigans.mime.models.DeviceData;
import com.shemanigans.mime.models.DeviceStatus;
import com.shemanigans.mime.models.Series;
import com.shemanigans.mime.services.BluetoothLeService;

public class Controlfragment extends BaseFragment
        implements View.OnClickListener { // This fragment presents information for the class LongTerm

    private static final String ARG_SECTION_NUMBER = "section_number";

    private Series.GenericSeries impMagSeries = new Series.GenericSeries("Ω");
    private Series.GenericSeries rSeries = new Series.GenericSeries("R");
    private Series.GenericSeries xSeries = new Series.GenericSeries("X");

    private DeviceStatus deviceStatus = BluetoothLeService.deviceStatus;

    // Declare UI references
    public TextView deviceAddress;
    public TextView mConnectionState, mDataField;
    public TextView sampleRateTextView, startFreqTextView, stepSizeTextView, numOfIncrementsTextView;
    public TextView summaryTableHeader;
    public View startButtonBar;
    public SwitchCompat enableNotifications;
    public ProgressBar connectionStateBar;
    public RelativeLayout textFileButtons;
    public TableLayout summaryTable;
    public Button exportToText, clearTextFile, startButton;
    public XYPlot bioimpedancePlot = null;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static Controlfragment newInstance(int sectionNumber) {
        Controlfragment fragment = new Controlfragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public Controlfragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dca, container, false);

        InitializeViewComponents(rootView);
        setSampleRateTextView(deviceStatus.sampleRate);
        setAcFreqTextViewParams(deviceStatus.startFreq, deviceStatus.stepSize, deviceStatus.numOfIncrements);

        setupPlot();

        /*if (longTerm) {
            bioimpedancePlot.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.GONE);
            startButtonBar.setVisibility(View.GONE);
            mConnectionState.setVisibility(View.GONE);
        }*/

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        deviceAddress = mConnectionState = mDataField =
                sampleRateTextView = startFreqTextView = stepSizeTextView =
                        numOfIncrementsTextView = summaryTableHeader = null;

        exportToText = clearTextFile = startButton = null;

        startButtonBar = null;
        enableNotifications = null;
        connectionStateBar = null;
        textFileButtons = null;
        summaryTable = null;
        bioimpedancePlot = null;
    }

    private void InitializeViewComponents(View rootView) {
        deviceAddress = (TextView) rootView.findViewById(R.id.device_address);
        exportToText = (Button) rootView.findViewById(R.id.export_to_text);
        clearTextFile = (Button) rootView.findViewById(R.id.clear_text_file);
        bioimpedancePlot = (XYPlot) rootView.findViewById(R.id.bioimpedancePlot);
        sampleRateTextView = (TextView) rootView.findViewById(R.id.sample_rate);
        startFreqTextView = (TextView) rootView.findViewById(R.id.start_freq);
        stepSizeTextView = (TextView) rootView.findViewById(R.id.step_size);
        numOfIncrementsTextView = (TextView) rootView.findViewById(R.id.num_of_increments);
        startButton = (Button) rootView.findViewById(R.id.begin);
        startButtonBar = rootView.findViewById(R.id.begin_bar);
        enableNotifications = (SwitchCompat) rootView.findViewById(R.id.enable_notifications);
        connectionStateBar = (ProgressBar) rootView.findViewById(R.id.connection_state_bar);
        textFileButtons = (RelativeLayout) rootView.findViewById(R.id.buttons);
        mConnectionState = (TextView) rootView.findViewById(R.id.connection_state);
        mDataField = (TextView) rootView.findViewById(R.id.data_value);
        summaryTableHeader = (TextView) rootView.findViewById(R.id.parameter_summary);
        summaryTable = (TableLayout) rootView.findViewById(R.id.summary_table);

        exportToText.setOnClickListener(this);
        clearTextFile.setOnClickListener(this);
        startButton.setOnClickListener(this);
        enableNotifications.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.begin:
                startSamplingInit();
                break;
            case R.id.enable_notifications:
                enableNotifications();
                break;
            case R.id.clear_text_file:
                clearTextFile();
                break;
            case R.id.export_to_text:
                exportToText();
                break;
        }
    }


    public void enableNotifications() {

        enableNotifications.setVisibility(View.VISIBLE);

        // Turn off notifications.
        if (!enableNotifications.isChecked()) {
            textFileButtons.setVisibility(View.VISIBLE);

            /*Intent intent = new Intent(getActivity(), ServiceBinder.class);
            getActivity().stopService(intent);*/
            getBleService().setCharacteristicNotification(SampleGattAttributes.BIOIMPEDANCE_DATA, false);
            getBleService().stopForeground(true);
        }

        // Turn on notifications.
        else {
            startSampling();
        }
    }


    public void startSamplingInit() {

        enableNotifications.setVisibility(View.VISIBLE);
        enableNotifications.setChecked(true);
        startButton.setVisibility(View.GONE);
        startButtonBar.setVisibility(View.GONE);

        startSampling();
    }

    public void startSampling() {

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.sampling_data))
                .setContentText(getText(R.string.connected));
        //.setContentIntent(activityPendingIntent);

        bioimpedancePlot.setVisibility(View.VISIBLE);
        textFileButtons.setVisibility(View.VISIBLE);

        getBleService().startForeground(BluetoothLeService.ONGOING_NOTIFICATION_ID, notificationBuilder.build());
        getBleService().setCharacteristicNotification(SampleGattAttributes.BIOIMPEDANCE_DATA, true);
    }


    public void exportToText() {

    }

    public void clearTextFile() {

    }

   /* public void startSampling() {
        buttonCallback.startSamplingInit();
    }*/

   /* public void enableNotifications() {
        buttonCallback.enableNotifications();
    }*/

    public void updateImpedanceData(String data) {
        mDataField.setText(data);
    }

    public void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    public void setDeviceAddressTextView(String mDeviceAddress) {
        deviceAddress.setText(mDeviceAddress);
    }


    public void setSampleRateTextView(int sampleRate) {
        sampleRateTextView.setText(getString(R.string.kilohertz, sampleRate));
    }

    public void setAcFreqTextViewParams(byte startFreq, byte stepSize, byte numOfIncrements) {
        startFreqTextView.setText(getString(R.string.kilohertz, startFreq));
        if (stepSize == 0) {
            stepSizeTextView.setText(R.string.not_applicable);
        }
        else {
            stepSizeTextView.setText(getString(R.string.kilohertz, stepSize));
        }
        if (numOfIncrements == 0) {
            numOfIncrementsTextView.setText(R.string.not_applicable);
        }
        else {
            numOfIncrementsTextView.setText(getString(R.string.kilohertz, numOfIncrements));
        }
    }

    public void updatePlot(DeviceData deviceData) {

        impMagSeries.addData(deviceData.impedance);
        rSeries.addData(deviceData.resistance);
        xSeries.addData(deviceData.reactance);

        // redraw the Plots:
        bioimpedancePlot.redraw();
    }

    public void onConnected() {
        updateConnectionState(R.string.connected);
        enableNotifications.setClickable(true);
        connectionStateBar.setVisibility(View.GONE);
        summaryTable.setVisibility(View.VISIBLE);
        summaryTableHeader.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        startButtonBar.setVisibility(View.VISIBLE);
    }

    public void onDisconnected() {
        updateConnectionState(R.string.disconnected);
        enableNotifications.setChecked(false);
        enableNotifications.setClickable(false);
        startButton.setVisibility(View.GONE);
        startButtonBar.setVisibility(View.GONE);
        summaryTable.setVisibility(View.GONE);
        summaryTableHeader.setVisibility(View.GONE);
        startButton.setTextColor(Color.WHITE);
        startButton.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primary));
    }

    public void gattConnecting() {
        connectionStateBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.primary),
                android.graphics.PorterDuff.Mode.SRC_IN);
        connectionStateBar.setVisibility(View.VISIBLE);
        updateConnectionState(R.string.connecting);
    }

    public void updateConnectionState(final int resourceId) {
        mConnectionState.setText(resourceId);
    }

    public void updatePlotSeries(boolean freqSweepOn) {
        if (freqSweepOn) {
            bioimpedancePlot.addSeries(rSeries, new LineAndPointFormatter(Color.parseColor("#008b8b"), null, null, null));
            bioimpedancePlot.addSeries(xSeries, new LineAndPointFormatter(Color.parseColor("#8b008b"), null, null, null));
        }
        else {
            bioimpedancePlot.removeSeries(rSeries);
            bioimpedancePlot.removeSeries(xSeries);
        }
    }

    private void setupPlot() {
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#fffafafa"));
        bgPaint.setStyle(Paint.Style.FILL);

        bioimpedancePlot.setRangeBoundaries(0, 1000, BoundaryMode.AUTO);
        bioimpedancePlot.setDomainBoundaries(0, 270, BoundaryMode.FIXED);

        // Format general area
        bioimpedancePlot.setBackgroundColor(Color.WHITE);
        bioimpedancePlot.getBackgroundPaint().set(bgPaint);
        //	bioimpedancePlot.getBackgroundPaint().setColor(Color.parseColor("#d8d8d8"));
        bioimpedancePlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.getGraphWidget().setGridBackgroundPaint(null);
        bioimpedancePlot.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
        bioimpedancePlot.getGraphWidget().setPadding(15, 15, 15, 15);
        bioimpedancePlot.setPlotMargins(0, 0, 0, 0);
        bioimpedancePlot.getTitleWidget().setText("");

        bioimpedancePlot.setBorderPaint(bgPaint);
        //bioimpedancePlot.getGraphWidget().getBorderPaint().setColor(Color.TRANSPARENT);

        // Format domain
        bioimpedancePlot.getDomainLabelWidget().getLabelPaint().setColor(Color.parseColor("#006bb2"));
        bioimpedancePlot.getDomainLabelWidget().getLabelPaint().setTextSize(20);
        bioimpedancePlot.getGraphWidget().getDomainTickLabelPaint().setColor(Color.BLACK);
        bioimpedancePlot.getGraphWidget().getDomainTickLabelPaint().setTextSize(20);
        bioimpedancePlot.getGraphWidget().getDomainOriginTickLabelPaint().setColor(Color.BLACK);
        bioimpedancePlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.setDomainStepValue(5);
        bioimpedancePlot.setDomainLabel("Sample Index");
        bioimpedancePlot.getDomainLabelWidget().pack();


        // Format range
        bioimpedancePlot.getRangeLabelWidget().getLabelPaint().setColor(Color.parseColor("#006bb2"));
        bioimpedancePlot.getRangeLabelWidget().getLabelPaint().setTextSize(20);
        bioimpedancePlot.getGraphWidget().getRangeTickLabelPaint().setColor(Color.BLACK);
        bioimpedancePlot.getGraphWidget().getRangeTickLabelPaint().setTextSize(20);
        bioimpedancePlot.getGraphWidget().getRangeOriginTickLabelPaint().setColor(Color.BLACK);
        bioimpedancePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        bioimpedancePlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.getGraphWidget().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.setRangeLabel("Data");
        bioimpedancePlot.setTicksPerRangeLabel(3);
        bioimpedancePlot.getRangeLabelWidget().pack();

        // Format legend

        bioimpedancePlot.getLegendWidget().getTextPaint().setColor(Color.parseColor("#006bb2"));
        bioimpedancePlot.getLegendWidget().getTextPaint().setTextSize(20);
        bioimpedancePlot.getLegendWidget().setPaddingBottom(10);

        // Add series

        bioimpedancePlot.addSeries(impMagSeries, new LineAndPointFormatter(Color.parseColor("#006bb2"), null, null, null));
        bioimpedancePlot.getBackgroundPaint().set(bgPaint);
    }


}
