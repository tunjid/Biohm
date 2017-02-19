package com.shemanigans.mime.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.shemanigans.mime.R;
import com.shemanigans.mime.SampleGattAttributes;
import com.shemanigans.mime.abstractclasses.BroadcastReceiverFragment;
import com.shemanigans.mime.dialogfragments.ConfirmationDialogFragment;
import com.shemanigans.mime.dialogfragments.FrequencySweepDialogFragment;
import com.shemanigans.mime.dialogfragments.SampleRateDialogFragment;
import com.shemanigans.mime.models.DeviceStatus;
import com.shemanigans.mime.services.BluetoothLeService;

import static android.content.Context.BIND_AUTO_CREATE;

public class OverviewParentFragment extends BroadcastReceiverFragment
        implements
        ServiceConnection,
        ViewPager.OnPageChangeListener,
        SampleRateDialogFragment.SampleRateListener,
        FrequencySweepDialogFragment.FrequencySweepListener {

    private static final int ACTIVITY = 0;
    private static final int CONTROL = 1;
    private static final int ANALYSIS = 2;

    //private boolean freqSweepOn = false;
    private boolean connected;

    private String deviceAddress;
    private DeviceStatus deviceStatus = BluetoothLeService.deviceStatus;
    private BluetoothLeService bluetoothLeService;

    public static OverviewParentFragment newInstance(String deviceName, String deviceAddress) {

        OverviewParentFragment fragment = new OverviewParentFragment();
        Bundle args = new Bundle();

        args.putString(BluetoothLeService.DEVICE_NAME, deviceName);
        args.putString(BluetoothLeService.DEVICE_ADDRESS, deviceAddress);
        fragment.setArguments(args);
        return fragment;
    }

    public OverviewParentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        deviceAddress = getArguments().getString(BluetoothLeService.DEVICE_ADDRESS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_longterm_dca, container, false);
        OverviewPagerAdapter analysisPagerAdapter = new OverviewPagerAdapter(getChildFragmentManager());
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.vpPager);
        TabLayout tabs = (TabLayout) rootView.findViewById(R.id.tabs);

        viewPager.setAdapter(analysisPagerAdapter);
        viewPager.addOnPageChangeListener(this);
        tabs.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(CONTROL);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent bleIntent = new Intent(getActivity(), BluetoothLeService.class);
        getActivity().bindService(bleIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_overview, menu);
        menu.findItem(R.id.menu_connect).setVisible(!connected);
        menu.findItem(R.id.menu_disconnect).setVisible(connected);

        Log.i("Test", "Connected? " + connected);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.set_sample_rate:
                if (bluetoothLeService != null) {
                    bluetoothLeService.setCharacteristicIndication(SampleGattAttributes.SAMPLE_RATE, true);

                    SampleRateDialogFragment sampleRateDialog = SampleRateDialogFragment.newInstance(deviceStatus.sampleRate);
                    sampleRateDialog.show(getChildFragmentManager(), "SampleRateFragment");
                }
                return true;
            case R.id.set_frequency_sweep:
                if (bluetoothLeService != null) {
                    bluetoothLeService.setCharacteristicIndication(SampleGattAttributes.AC_FREQ, true);

                    FrequencySweepDialogFragment frequencySweepDialog =
                            FrequencySweepDialogFragment.newInstance(deviceStatus.startFreq,
                                    deviceStatus.stepSize, deviceStatus.numOfIncrements);

                    frequencySweepDialog.show(getChildFragmentManager(), "FrequencySweepFragment");
                }
                return true;
            case R.id.stop_background_service:
                if (bluetoothLeService != null) {
                    bluetoothLeService.stopSelf();
                }
                return true;
            case R.id.menu_connect:
                bluetoothLeService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bluetoothLeService.stopForeground(true);
                bluetoothLeService.disconnect();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case BluetoothLeService.GATT_CONNECTED:
                connected = true;
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_CONNECTING:
                connected = false;
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_DISCONNECTED:

                connected = false;
                bluetoothLeService.stopForeground(true);
                //bluetoothLeService.close();
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.DATA_AVAILABLE_SAMPLE_RATE:
                dismissConfirmationDialog();
                break;
            case BluetoothLeService.DATA_AVAILABLE_FREQUENCY_PARAMS:
                dismissConfirmationDialog();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Nothing to implement.
    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case ANALYSIS:
                AnalysisFragment analysisFragment = getAnalysisFragment();
                analysisFragment.setup();
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // Nothing to implement.
    }

    @Override
    public void onDialogPositiveClickSampleRate(byte sampleRate) {
        try {
            ConfirmationDialogFragment confirmationDialog = new ConfirmationDialogFragment();
            confirmationDialog.show(getChildFragmentManager(), "ConfirmationFragment");
            deviceStatus.sampleRate = sampleRate;

            if (getControlFragment() != null) {
                getControlFragment().setSampleRateTextView(sampleRate);
            }
            if (bluetoothLeService != null) {
                bluetoothLeService.writeCharacteristic(SampleGattAttributes.SAMPLE_RATE, sampleRate);
                Toast.makeText(getContext(), "New frequency: " + sampleRate, Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            Toast.makeText(getContext(), R.string.set_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDialogPositiveClickFrequencySweep(byte startFreq, byte stepSize, byte numOfIncrements) {
        // User touched the dialog's positive button
        // Set Value
        try {
            ConfirmationDialogFragment confirmationDialog = new ConfirmationDialogFragment();
            confirmationDialog.show(getChildFragmentManager(), "ConfirmationFragment");

            int endFreq = ((int) startFreq) + (stepSize * numOfIncrements);

            boolean isInvalidInput = endFreq > 127 || stepSize > 125 || numOfIncrements < 0 ||
                    startFreq == 0 || stepSize == 0 || numOfIncrements == 0;

            if (isInvalidInput) {
                System.out.println("Too large an end freq");
                BluetoothLeService.showToast(getContext(), R.string.check_value);
                return;
            }

            byte[] freqValuesByte = {startFreq, stepSize, numOfIncrements};

            if (getControlFragment() != null) {
                getControlFragment().setAcFreqTextViewParams(startFreq, stepSize, numOfIncrements);
            }
            if (bluetoothLeService != null) {
                bluetoothLeService.writeCharacteristicArray(SampleGattAttributes.AC_FREQ, freqValuesByte);
            }

            Toast.makeText(getContext(), R.string.freq_sweep_enabled, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDialogNeutralClickFrequencySweep() {
        Toast.makeText(getContext(), R.string.cancelled, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDialogNegativeClickFrequencySweep(byte startFreq) {

        if (startFreq == 0) {
            Toast.makeText(getContext(), R.string.no_zero, Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmationDialogFragment confirmationDialog = new ConfirmationDialogFragment();
        confirmationDialog.show(getChildFragmentManager(), "ConfirmationFragment");

        byte[] freqValuesByte = {startFreq, 0, 0};

        if (bluetoothLeService != null) {
            bluetoothLeService.writeCharacteristicArray(SampleGattAttributes.AC_FREQ, freqValuesByte);
        }
        if (getControlFragment() != null) {
            getControlFragment().setAcFreqTextViewParams(startFreq, deviceStatus.stepSize,
                    deviceStatus.numOfIncrements);
        }

        Toast.makeText(getContext(), R.string.freq_sweep_disabled, Toast.LENGTH_SHORT).show();
    }

    private void dismissConfirmationDialog() {
        ConfirmationDialogFragment confirmationDialog = getConfirmationDialogFragment();

        if (confirmationDialog != null) {
            confirmationDialog.dismiss();
        }
    }

    private ConfirmationDialogFragment getConfirmationDialogFragment() {
        return (ConfirmationDialogFragment)
                getChildFragmentManager().findFragmentByTag("ConfirmationFragment");
    }

    private Controlfragment getControlFragment() {
        return (Controlfragment) getChildFragmentManager()
                .findFragmentByTag(getFragmentTag(CONTROL));
    }

    private AnalysisFragment getAnalysisFragment() {
        return (AnalysisFragment) getChildFragmentManager()
                .findFragmentByTag(getFragmentTag(ANALYSIS));
    }


    public static String getFragmentTag(int index) {
        return "android:switcher:" + R.id.vpPager + ":" + index;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bluetoothLeService = null;
    }

    static class OverviewPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 3;

        OverviewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case ACTIVITY: // Fragment # 0 - This will show FirstFragment different title
                    return ActivityFragment.newInstance(position);
                case CONTROL: // Fragment # 0 - This will show FirstFragment
                    return Controlfragment.newInstance(position);
                case ANALYSIS: // Fragment # 1 - This will show SecondFragment
                    return AnalysisFragment.newInstance(position);
                default:
                    return Controlfragment.newInstance(1);
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case ACTIVITY:
                    return "Activity";
                case CONTROL:
                    return "Raw Data";
                case ANALYSIS:
                    return "Analysis";
                default:
                    return "Page " + position;
            }
        }

    }


    /*
    protected void onReceive(Context context, Intent intent) {

        Controlfragment controlfragment = getControlFragment();
        ActivityFragment activityFragment = getActivityFragment();
        AnalysisFragment analysisFragment = getAnalysisFragment();

        final String action = intent.getAction();

        switch (action) {
            case BluetoothLeService.GATT_CONNECTED:
                connected = true;
                controlfragment.onConnected();
                controlfragment.setDeviceAddressTextView(BluetoothLeService.deviceAddress);
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_CONNECTING:
                connected = false;
                controlfragment.gattConnecting();
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_DISCONNECTED:

                connected = false;
                controlfragment.onDisconnected();
                controlfragment.clearUI();
                bluetoothLeService.stopForeground(true);
                //bluetoothLeService.close();
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.DATA_AVAILABLE_UNKNOWN:
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                controlfragment.mDataField.setText(data);
                break;
            case BluetoothLeService.DATA_AVAILABLE_SAMPLE_RATE:
                controlfragment.setSampleRateTextView(deviceStatus.sampleRate);

                dismissConfirmationDialog();
                break;
            case BluetoothLeService.DATA_AVAILABLE_FREQUENCY_PARAMS:

                controlfragment.startFreqTextView.setText(getString(R.string.kilohertz, deviceStatus.startFreq));

                if (!deviceStatus.freqSweepOn) {
                    controlfragment.stepSizeTextView.setText(R.string.not_applicable);
                    controlfragment.numOfIncrementsTextView.setText(R.string.not_applicable);
                    analysisFragment.setup();
                    controlfragment.updatePlotSeries(false);
                }
                else {
                    controlfragment.stepSizeTextView.setText(getString(R.string.kilohertz, deviceStatus.stepSize));
                    controlfragment.numOfIncrementsTextView.setText(getString(R.string.number, deviceStatus.numOfIncrements));

                    analysisFragment.setup();
                    controlfragment.updatePlotSeries(true);
                }

                dismissConfirmationDialog();
                break;
            case BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE:
                DeviceData deviceData = intent.getParcelableExtra(BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE);
                String impedanceData = deviceData.getAsCSV();

                // update instantaneous data:
                controlfragment.updateImpedanceData(impedanceData);
                controlfragment.updatePlot(deviceData);
                activityFragment.updatePlot(deviceData);
                break;
            case BluetoothLeService.DATA_AVAILABLE_TAUBIN_SOLUTION:
                TaubinSolution taubinSolution = intent.getParcelableExtra(BluetoothLeService.DATA_AVAILABLE_TAUBIN_SOLUTION);
                analysisFragment.onTaubinSolution(taubinSolution);
                break;
        }
    }
     */
}
