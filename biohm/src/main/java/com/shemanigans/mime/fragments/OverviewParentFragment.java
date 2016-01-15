package com.shemanigans.mime.fragments;

import android.content.Intent;
import android.os.Bundle;
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
import com.shemanigans.mime.abstractclasses.BaseFragment;
import com.shemanigans.mime.dialogfragments.ConfirmationDialogFragment;
import com.shemanigans.mime.dialogfragments.FrequencySweepDialogFragment;
import com.shemanigans.mime.dialogfragments.SampleRateDialogFragment;
import com.shemanigans.mime.models.DeviceData;
import com.shemanigans.mime.models.DeviceStatus;
import com.shemanigans.mime.models.TaubinSolution;
import com.shemanigans.mime.services.BluetoothLeService;

public class OverviewParentFragment extends BaseFragment
        implements
        SampleRateDialogFragment.SampleRateListener,
        FrequencySweepDialogFragment.FrequencySweepListener,
        ViewPager.OnPageChangeListener {

    private static final String ARG_SECTION_NUMBER = "ARG_SECTION_NUMBER";

    private static final int ACTIVITY = 0;
    private static final int CONTROL = 1;
    private static final int ANALYSIS = 2;

    //private boolean freqSweepOn = false;
    private boolean mConnected = false;

    private DeviceStatus deviceStatus = BluetoothLeService.deviceStatus;

    public static OverviewParentFragment newInstance(int sectionNumber,
                                                     String deviceName,
                                                     String deviceAddress) {

        OverviewParentFragment fragment = new OverviewParentFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_longterm_dca, container, false);
        InitializeViewComponents(rootView);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_overview, menu);
        menu.findItem(R.id.menu_connect).setVisible(!mConnected);
        menu.findItem(R.id.menu_disconnect).setVisible(mConnected);

        Log.i("Test","Connected? " + mConnected);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.set_sample_rate:
                if (getBleService() != null) {
                    getBleService().setCharacteristicIndication(SampleGattAttributes.SAMPLE_RATE, true);
                    SampleRateDialogFragment sampleRateDialog = SampleRateDialogFragment.newInstance(deviceStatus.sampleRate);
                    sampleRateDialog.show(getChildFragmentManager(), "SampleRateFragment");
                }
                return true;
            case R.id.set_frequency_sweep:
                if (getBleService() != null) {
                    getBleService().setCharacteristicIndication(SampleGattAttributes.AC_FREQ, true);

                    FrequencySweepDialogFragment frequencySweepDialog =
                            FrequencySweepDialogFragment.newInstance(deviceStatus.startFreq,
                                    deviceStatus.stepSize, deviceStatus.numOfIncrements);

                    frequencySweepDialog.show(getChildFragmentManager(), "FrequencySweepFragment");
                }
                return true;
            case R.id.stop_background_service:
                if (getBleService() != null) {
                    getBleService().stopSelf();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void receiveBroadcast(Intent intent) {

        Controlfragment controlfragment = getControlFragment();
        ActivityFragment activityFragment = getActivityFragment();
        AnalysisFragment analysisFragment = getAnalysisFragment();

        final String action = intent.getAction();

        switch (action) {
            case BluetoothLeService.GATT_CONNECTED:
                mConnected = true;
                controlfragment.onConnected();
                controlfragment.setDeviceAddressTextView(BluetoothLeService.deviceAddress);
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_CONNECTING:
                mConnected = false;
                controlfragment.gattConnecting();
                getActivity().invalidateOptionsMenu();
                break;
            case BluetoothLeService.GATT_DISCONNECTED:

                BluetoothLeService bluetoothLeService = getBleService();

                mConnected = false;
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
                    analysisFragment.setupFragment();
                    controlfragment.updatePlotSeries(false);
                }
                else {
                    controlfragment.stepSizeTextView.setText( getString(R.string.kilohertz, deviceStatus.stepSize));
                    controlfragment.numOfIncrementsTextView.setText(getString(R.string.number, deviceStatus.numOfIncrements));

                    analysisFragment.setupFragment();
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

    private void InitializeViewComponents(View rootView) {

        final OverviewPagerAdapter analysisPagerAdapter = new OverviewPagerAdapter(getChildFragmentManager());
        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.vpPager);
        final TabLayout tabs = (TabLayout) rootView.findViewById(R.id.tabs);

        viewPager.setAdapter(analysisPagerAdapter);
        viewPager.addOnPageChangeListener(this);
        tabs.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(CONTROL);
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
                analysisFragment.setupFragment();
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
            if (getBleService() != null) {
                getBleService().writeCharacteristic(SampleGattAttributes.SAMPLE_RATE, sampleRate);
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
            if (getBleService() != null) {
                getBleService().writeCharacteristicArray(SampleGattAttributes.AC_FREQ, freqValuesByte);
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

        if (getBleService() != null) {
            getBleService().writeCharacteristicArray(SampleGattAttributes.AC_FREQ, freqValuesByte);
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

    private ActivityFragment getActivityFragment() {
        return (ActivityFragment) getChildFragmentManager()
                .findFragmentByTag(getFragmentTag(ACTIVITY));
    }

    public static String getFragmentTag(int index) {
        return "android:switcher:" + R.id.vpPager + ":" + index;
    }

    public static class OverviewPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 3;

        public OverviewPagerAdapter(FragmentManager fragmentManager) {

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

}
