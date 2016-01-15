package com.shemanigans.mime.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A point on a resistance reactance curve.
 */
public class RXpair implements Parcelable {
    public double resistance;
    public double reactance;

    public RXpair() {

    }

    public RXpair(DeviceData deviceData) {
        this.resistance = deviceData.resistance;
        this.reactance = deviceData.reactance;
    }

    public RXpair(double resistance, double reactance) {
        this.resistance = resistance;
        this.reactance = reactance;
    }

    protected RXpair(Parcel in) {
        resistance = in.readDouble();
        reactance = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(resistance);
        dest.writeDouble(reactance);
    }

    public static final Parcelable.Creator<RXpair> CREATOR = new Parcelable.Creator<RXpair>() {
        @Override
        public RXpair createFromParcel(Parcel in) {
            return new RXpair(in);
        }

        @Override
        public RXpair[] newArray(int size) {
            return new RXpair[size];
        }
    };
}
