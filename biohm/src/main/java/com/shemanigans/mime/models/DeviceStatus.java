package com.shemanigans.mime.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class for holding all parameters describing the status of the AD5933
 */
public class DeviceStatus implements Parcelable {
    public boolean freqSweepOn;
    public byte sampleRate = 50;
    public byte startFreq = 0;
    public byte stepSize = 0;
    public byte numOfIncrements = 0;
    public int endFreq = 0;

    public DeviceStatus() {

    }

    protected DeviceStatus(Parcel in) {
        freqSweepOn = in.readByte() != 0x00;
        sampleRate = in.readByte();
        startFreq = in.readByte();
        stepSize = in.readByte();
        numOfIncrements = in.readByte();
        endFreq = in.readInt();
    }

    public void updateFrequencyVariables() {
        freqSweepOn = !(stepSize == 0);
        endFreq = ((int) startFreq) + (stepSize * numOfIncrements);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (freqSweepOn ? 0x01 : 0x00));
        dest.writeByte(sampleRate);
        dest.writeByte(startFreq);
        dest.writeByte(stepSize);
        dest.writeByte(numOfIncrements);
        dest.writeInt(endFreq);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DeviceStatus> CREATOR = new Parcelable.Creator<DeviceStatus>() {
        @Override
        public DeviceStatus createFromParcel(Parcel in) {
            return new DeviceStatus(in);
        }

        @Override
        public DeviceStatus[] newArray(int size) {
            return new DeviceStatus[size];
        }
    };
}
