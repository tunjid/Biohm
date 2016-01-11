package com.shemanigans.mime.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shemanigans.mime.R;

import java.util.ArrayList;

/**
 * Adapter for BLE devices found while sacnning
 */
// Adapter for holding devices found through scanning
public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ViewHolder> {

    private static final int BLE_DEVICE = 1;

    private ArrayList<BluetoothDevice> bleDevices;
    private AdapterListener adapterListener;

    public ScanAdapter(AdapterListener adapterListener, ArrayList<BluetoothDevice> bleDevices) {
        this.adapterListener = adapterListener;
        this.bleDevices = bleDevices;
    }

    public void clear() {
        bleDevices.clear();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        View itemView = LayoutInflater.from(context).inflate(R.layout.activity_scan_textview_holder, viewGroup, false);

        return new ViewHolder(itemView, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        final BluetoothDevice device = bleDevices.get(position);
        final String deviceName = device.getName();

        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        }
        else {
            viewHolder.deviceName.setText(R.string.unknown_device);
        }

        viewHolder.deviceAddress.setText(device.getAddress());

        viewHolder.viewHolderListener = new ViewHolder.ViewHolderListener() {
            @Override
            public void onBluetoothDeviceClicked() {
                adapterListener.onBluetoothDeviceClicked(device);
            }
        };

    }

    @Override
    public int getItemViewType(int position) {
        return BLE_DEVICE;
    }

    @Override
    public int getItemCount() {
        return bleDevices.size();
    }

    // ViewHolder for actual content
    public final static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public int viewType;        // Used to specify the view type.

        public TextView deviceName;
        public TextView deviceAddress;

        public ViewHolderListener viewHolderListener;

        public ViewHolder(View itemView, int ViewType) {
            super(itemView);
            this.viewType = ViewType;

            switch (ViewType) {
                default:
                    deviceAddress = (TextView) itemView.findViewById(R.id.device_address);
                    deviceName = (TextView) itemView.findViewById(R.id.device_name);
                    itemView.setOnClickListener(this); // Set listener for the whole relative layout
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            switch ((v.getId())) {
                case R.id.row_parent:
                    viewHolderListener.onBluetoothDeviceClicked();
                    break;
            }
        }

        public interface ViewHolderListener {
            void onBluetoothDeviceClicked();
        }
    }

    public interface AdapterListener {
        void onBluetoothDeviceClicked(BluetoothDevice bluetoothDevice);
    }

}
