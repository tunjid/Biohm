package com.shemanigans.mime.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import com.androidplot.ui.widget.TextLabelWidget;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.shemanigans.mime.R;
import com.shemanigans.mime.SampleGattAttributes;
import com.shemanigans.mime.abstractclasses.BroadcastReceiverFragment;
import com.shemanigans.mime.models.DeviceData;
import com.shemanigans.mime.models.DeviceStatus;
import com.shemanigans.mime.models.Series;
import com.shemanigans.mime.services.BluetoothLeService;

import static android.content.Context.BIND_AUTO_CREATE;

public class Controlfragment extends BroadcastReceiverFragment
        implements
        ServiceConnection,
        View.OnClickListener { // This fragment presents information for the class LongTerm

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
    public Switch enableNotifications;
    public ProgressBar connectionStateBar;
    public TableLayout summaryTable;
    public Button exportToText, clearTextFile, startButton;
    public XYPlot bioimpedancePlot = null;

    private BluetoothLeService bluetoothLeService;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_control, container, false);

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
        enableNotifications = (Switch) rootView.findViewById(R.id.enable_notifications);
        connectionStateBar = (ProgressBar) rootView.findViewById(R.id.connection_state_bar);
        mConnectionState = (TextView) rootView.findViewById(R.id.connection_state);
        mDataField = (TextView) rootView.findViewById(R.id.data_value);
        summaryTableHeader = (TextView) rootView.findViewById(R.id.parameter_summary);
        summaryTable = (TableLayout) rootView.findViewById(R.id.summary_table);

        exportToText.setOnClickListener(this);
        clearTextFile.setOnClickListener(this);
        startButton.setOnClickListener(this);
        enableNotifications.setOnClickListener(this);

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent bleIntent = new Intent(getActivity(), BluetoothLeService.class);
        getActivity().bindService(bleIntent, this, BIND_AUTO_CREATE);
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
        summaryTable = null;
        bioimpedancePlot = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bluetoothLeService = null;
    }

    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BluetoothLeService.GATT_CONNECTED:
                onConnected();
                setDeviceAddressTextView(bluetoothLeService.getDeviceAddress());
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_CONNECTING:
                gattConnecting();
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_DISCONNECTED:
                onDisconnected();
                clearUI();
                break;
            case BluetoothLeService.DATA_AVAILABLE_UNKNOWN:
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                mDataField.setText(data);
                break;
            case BluetoothLeService.DATA_AVAILABLE_SAMPLE_RATE:
                setSampleRateTextView(deviceStatus.sampleRate);
                break;
            case BluetoothLeService.DATA_AVAILABLE_FREQUENCY_PARAMS:

                startFreqTextView.setText(getString(R.string.kilohertz, deviceStatus.startFreq));

                if (!deviceStatus.freqSweepOn) {
                    stepSizeTextView.setText(R.string.not_applicable);
                    numOfIncrementsTextView.setText(R.string.not_applicable);

                    updatePlotSeries(false);
                }
                else {
                    stepSizeTextView.setText(getString(R.string.kilohertz, deviceStatus.stepSize));
                    numOfIncrementsTextView.setText(getString(R.string.number, deviceStatus.numOfIncrements));

                    updatePlotSeries(true);
                }
                break;
            case BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE:
                DeviceData deviceData = intent.getParcelableExtra(BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE);
                String impedanceData = deviceData.getAsCSV();

                // update instantaneous data:
                updateImpedanceData(impedanceData);
                updatePlot(deviceData);
                break;
        }
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
            //textFileButtons.setVisibility(View.VISIBLE);

            /*Intent intent = new Intent(getActivity(), ServiceBinder.class);
            getActivity().stopService(intent);*/

            bluetoothLeService.stopForeground(true);
            bluetoothLeService.setCharacteristicNotification(SampleGattAttributes.BIOIMPEDANCE_DATA, false);

            //getBleService().setCharacteristicNotification(SampleGattAttributes.BIOIMPEDANCE_DATA, false);
            //getBleService().stopForeground(true);
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
        //textFileButtons.setVisibility(View.VISIBLE);

        bluetoothLeService.startForeground(BluetoothLeService.ONGOING_NOTIFICATION_ID, notificationBuilder.build());
        bluetoothLeService.setCharacteristicNotification(SampleGattAttributes.BIOIMPEDANCE_DATA, true);
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

        XYGraphWidget graphWidget = bioimpedancePlot.getGraph();

        bioimpedancePlot.setRangeBoundaries(0, 1000, BoundaryMode.AUTO);
        bioimpedancePlot.setDomainBoundaries(0, 270, BoundaryMode.FIXED);

        // Format general area
        bioimpedancePlot.setBackgroundColor(Color.WHITE);
        bioimpedancePlot.getBackgroundPaint().set(bgPaint);
        //	bioimpedancePlot.getBackgroundPaint().setColor(Color.parseColor("#d8d8d8"));
        graphWidget.getBackgroundPaint().setColor(Color.TRANSPARENT);
        graphWidget.setGridBackgroundPaint(null);
        bioimpedancePlot.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
        graphWidget.setPadding(15, 15, 15, 15);
        bioimpedancePlot.setPlotMargins(0, 0, 0, 0);
        bioimpedancePlot.getTitle().setText("");

        bioimpedancePlot.setBorderPaint(bgPaint);
        //graphWidget.getBorderPaint().setColor(Color.TRANSPARENT);

        // Format domain

//        graphWidget.getDomainTickLabelPaint().setColor(Color.BLACK);
//        graphWidget.getDomainTickLabelPaint().setTextSize(20);
//        graphWidget.getDomainOriginTickLabelPaint().setColor(Color.BLACK);
        graphWidget.getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.setDomainStepValue(5);
        bioimpedancePlot.setDomainLabel("Sample Index");

        TextLabelWidget domainTitle = bioimpedancePlot.getDomainTitle();

        domainTitle.getLabelPaint().setColor(Color.parseColor("#006bb2"));
        domainTitle.getLabelPaint().setTextSize(20);
        domainTitle.pack();


        // Format range
        bioimpedancePlot.getRangeTitle().getLabelPaint().setColor(Color.parseColor("#006bb2"));
        bioimpedancePlot.getRangeTitle().getLabelPaint().setTextSize(20);
//        graphWidget.getRangeTickLabelPaint().setColor(Color.BLACK);
//        graphWidget.getRangeTickLabelPaint().setTextSize(20);
//        graphWidget.getRangeOriginTickLabelPaint().setColor(Color.BLACK);
        graphWidget.getRangeOriginLinePaint().setColor(Color.BLACK);
        graphWidget.getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        graphWidget.getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        bioimpedancePlot.setRangeLabel("Data");
//        bioimpedancePlot.setTicksPerRangeLabel(3);
        bioimpedancePlot.getRangeTitle().pack();

        // Format legend

        bioimpedancePlot.getLegend().getTextPaint().setColor(Color.parseColor("#006bb2"));
        bioimpedancePlot.getLegend().getTextPaint().setTextSize(20);
        bioimpedancePlot.getLegend().setPaddingBottom(10);

        // Add series

        bioimpedancePlot.addSeries(impMagSeries, new LineAndPointFormatter(Color.parseColor("#006bb2"), null, null, null));
        bioimpedancePlot.getBackgroundPaint().set(bgPaint);
    }


}
