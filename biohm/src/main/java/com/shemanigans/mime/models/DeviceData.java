package com.shemanigans.mime.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wrapper for data coming in over the BLE connection
 */

public class DeviceData implements Parcelable {

    public byte accelX;
    public byte accelY;
    public byte accelZ;
    public byte freqKHZ;
    public double impedance;
    public double resistance;
    public double reactance;
    public double phaseAngle;

    public String getAsCSV() {

        return "" + accelX + "," + accelY + "," + accelZ
                + "," + impedance + "," + phaseAngle + "," + freqKHZ;

    }

    public RXpair getRXpair() {
        return new RXpair(resistance, reactance);
    }

    public DeviceData(byte[] rawData) {

        final StringBuilder impVal = new StringBuilder("");

        int phaseAngleUnit;
        int phaseAngleTenth;
        int phaseAngleThousandth;
        double phaseAngleWhole;

        accelX = rawData[0];
        accelY = rawData[1];
        accelZ = rawData[2];

        impVal.append(rawData[3] < 10
                ? "0" + rawData[3]
                : rawData[3]);

        impVal.append(rawData[4] < 10
                ? "0" + rawData[4]
                : rawData[4]);

        impVal.append(rawData[5] < 10
                ? "0" + rawData[5]
                : rawData[5]);

        impVal.append(rawData[6] < 10
                ? "0" + rawData[6]
                : rawData[6]);

        phaseAngleUnit = rawData[7];
        phaseAngleTenth = rawData[8];
        phaseAngleThousandth = rawData[9];
        freqKHZ = rawData[10];


        if (phaseAngleThousandth > 0) {
            phaseAngleWhole = (phaseAngleUnit * 10000);
            phaseAngleTenth *= 100;
            phaseAngleWhole += phaseAngleTenth;
            phaseAngleWhole += phaseAngleThousandth;
            phaseAngleWhole /= 10000;
        }
        else {
            phaseAngleWhole = (phaseAngleUnit * 10000);
            phaseAngleTenth *= 100;
            phaseAngleWhole += phaseAngleTenth;
            phaseAngleWhole += Math.abs(phaseAngleThousandth);
            phaseAngleWhole /= -10000;
        }

        double actualVal = Double.parseDouble(impVal.toString());
        actualVal /= 10000;

        impedance = actualVal;
        resistance = actualVal * Math.cos(phaseAngleWhole);
        reactance = Math.abs(actualVal * Math.sin(phaseAngleWhole));
    }

    protected DeviceData(Parcel in) {
        accelX = in.readByte();
        accelY = in.readByte();
        accelZ = in.readByte();
        freqKHZ = in.readByte();
        impedance = in.readDouble();
        resistance = in.readDouble();
        reactance = in.readDouble();
        phaseAngle = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(accelX);
        dest.writeByte(accelY);
        dest.writeByte(accelZ);
        dest.writeByte(freqKHZ);
        dest.writeDouble(impedance);
        dest.writeDouble(resistance);
        dest.writeDouble(reactance);
        dest.writeDouble(phaseAngle);
    }

    public static final Parcelable.Creator<DeviceData> CREATOR = new Parcelable.Creator<DeviceData>() {
        @Override
        public DeviceData createFromParcel(Parcel in) {
            return new DeviceData(in);
        }

        @Override
        public DeviceData[] newArray(int size) {
            return new DeviceData[size];
        }
    };
}
