package com.shemanigans.mime.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import rx.Observable;
import rx.functions.Func0;

/**
 * An object containing the solution to a tabin circle fit
 */
@Setter
public class TaubinSolution implements Parcelable, XYSeries {

    public double rNaught;
    public double rInfinity;
    public double alpha;
    public double re;
    public double ri;
    public double tau;
    public double fPeak;
    public double c;

    public double RMSE;
    public double RSQ;

    public double xOffset;
    public double rOffset;
    public double radius;
    public ArrayList<RXpair> measuredRXPairs = new ArrayList<>(100);
    public ArrayList<RXpair> fittedRXPairs = new ArrayList<>(100);

    private TaubinSolution() {

    }

    public static Observable<TaubinSolution> taubinFit(final ArrayList<RXpair> circleCoordinates, final double endFrequency) {

        // Defer so this isn't called till the observable is subscribed to.
        return rx.Observable.defer(new Func0<Observable<TaubinSolution>>() {
            @Override
            public rx.Observable<TaubinSolution> call() {

                TaubinSolution solution = new TaubinSolution();

                solution.setMeasuredRXPairs(circleCoordinates);

                int size = circleCoordinates.size();     //  number of data points

                double Mxy, Mxx, Myy, Mxz, Myz, Mzz, Mz, Cov_xy;
                double Xi, Yi, Zi;
                double A0, A1, A2, A22, A3, A33;
                double DET, Dy, xnew, xold, ynew, yold;
                double epsilon = 1e-12;
                double iterMax = 20;
                double xBar = 0;
                double TSS, RSS, dRSS, dTSS;

                RXpair center = new RXpair();   //  the centroid of the data set
                RXpair centroid = circleMidpoints(circleCoordinates);   //  the centroid of the data set

                Mxx = Myy = Mxy = Mxz = Myz = Mzz = 0;
                xnew = 0;
                TSS = RSS = 0;
                ynew = 1e+20;

                // computing moments (note: all moments will be normed, i.e. divided by n)

                for (RXpair circleCoordinate : circleCoordinates) {
                    Xi = circleCoordinate.resistance - centroid.resistance;  //   centering data
                    Yi = circleCoordinate.reactance - centroid.reactance;  //   centering data
                    Zi = (Xi * Xi) + (Yi * Yi);
                    xBar += circleCoordinate.resistance;
                    Mxy += Xi * Yi;
                    Mxx += Xi * Xi;
                    Myy += Yi * Yi;
                    Mxz += Xi * Zi;
                    Myz += Yi * Zi;
                    Mzz += Zi * Zi;
                }
                xBar /= size;
                Mxx /= size;
                Myy /= size;
                Mxy /= size;
                Mxz /= size;
                Myz /= size;
                Mzz /= size;

                // Compute the coefficients of the characteristic polynomial

                Mz = Mxx + Myy;
                Cov_xy = (Mxx * Myy) - (Mxy * Mxy);
                A3 = 4 * Mz;
                A2 = (-3 * Mz * Mz) - Mzz;
                A1 = (Mzz * Mz) + (4 * Cov_xy * Mz) - (Mxz * Mxz) - (Myz * Myz) - (Mz * Mz * Mz);
                A0 = (Mxz * Mxz * Myy) + (Myz * Myz * Mxx) - (Mzz * Cov_xy) - (2 * Mxz * Myz * Mxy) + (Mz * Mz * Cov_xy);
                A22 = A2 + A2;
                A33 = A3 + A3 + A3;

                //  Newton's method starting at x = 0

                for (int i = 0; i <= iterMax; i++) {
                    yold = ynew;
                    ynew = A0 + xnew * (A1 + xnew * (A2 + xnew * A3));
                    if (Math.abs(ynew) > Math.abs(yold)) {
                        xnew = 0;
                        break;
                    }
                    Dy = A1 + xnew * (A22 + xnew * A33);
                    xold = xnew;
                    xnew = xold - ynew / Dy;
                    if (Math.abs((xnew - xold) / xnew) < epsilon) {
                        break;
                    }
                    if (i >= iterMax) {
                        xnew = 0;
                    }
                    if (xnew < 0) {
                        xnew = 0;
                    }
                }

                // Compute circle parameters

                DET = (xnew * xnew) - (xnew * Mz) + Cov_xy;
                center.resistance = (Mxz * (Myy - xnew) - Myz * Mxy) / DET / 2;
                center.reactance = (Myz * (Mxx - xnew) - Mxz * Mxy) / DET / 2;

                double rOffset = center.resistance + centroid.resistance;
                double xOffset = center.reactance + centroid.reactance;
                double radius = Math.sqrt(((center.resistance * center.resistance) + (center.reactance * center.reactance)) + Mz);

                double sampledR = (circleCoordinates.get(size - 1).resistance);
                double fittedX = (fittedX(sampledR, radius, rOffset, xOffset));

                solution.fittedRXPairs.clear();

                // Find root mean square error and R squared

                for (RXpair circleCoordinate : circleCoordinates) {

                    double fittedReactance = fittedX(circleCoordinate.resistance, radius, rOffset, xOffset);

                    solution.fittedRXPairs.add(new RXpair(circleCoordinate.resistance, fittedReactance));

                    dRSS = (circleCoordinate.reactance - fittedReactance);
                    dTSS = circleCoordinate.reactance - xBar;
                    RSS += Math.pow(dRSS, 2);
                    TSS += Math.pow(dTSS, 2);
                }

                solution.setRMSE(Math.sqrt(RSS / size));
                solution.setRSQ(1 - (RSS / TSS));


                // Solved set parameters

                double[] resistances = quadratic(rOffset, xOffset, radius);

                solution.setXOffset(xOffset);
                solution.setROffset(rOffset);
                solution.setRadius(radius);

                solution.setRNaught(resistances[0]);
                solution.setRInfinity(resistances[1]);
                solution.setAlpha((2 / Math.PI) * Math.atan((solution.rNaught - rOffset) / (-1 * xOffset)));
                solution.setRe(solution.rNaught);
                solution.setRi((solution.rNaught * solution.rInfinity) / (solution.rNaught - solution.rInfinity));

                solution.setTau((1 / (2 * Math.PI * 1000 * endFrequency)) * Math.pow((solution.rNaught - sampledR)
                        / (fittedX - xOffset), 1 / solution.alpha));

                solution.setFPeak(1 / (2000 * solution.tau * Math.PI));
                solution.setC(solution.tau / (solution.re + solution.ri));

                return Observable.just(solution);
            }
        });
    }

    private static RXpair circleMidpoints(List<RXpair> rXpairs) {
        RXpair answer = new RXpair();

        double resistanceMean = 0;
        double reactanceMean = 0;

        for (RXpair xyCoordinate : rXpairs) {
            resistanceMean += xyCoordinate.resistance;
            reactanceMean += xyCoordinate.reactance;
        }
        resistanceMean /= rXpairs.size();
        reactanceMean /= rXpairs.size();

        answer.resistance = resistanceMean;
        answer.reactance = reactanceMean;

        return answer;
    }

    private static double fittedX(double resistance, double radius, double R_Offset, double X_Offset) {
        double answer;
        answer = Math.sqrt(Math.pow(radius, 2) - Math.pow((resistance - R_Offset), 2)) + X_Offset;
        return answer;
    }

    private static double[] quadratic(double p, double q, double s) {
        double[] answer = {1, 1};
        answer[0] = ((2 * p) + Math.sqrt((4 * p * p) - (4 * (p * p + q * q - s * s)))) / 2;
        answer[1] = ((2 * p) - Math.sqrt((4 * p * p) - (4 * (p * p + q * q - s * s)))) / 2;
        return answer;
    }


    /*public StringBuilder appendAnalysisData() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%.1f", this.re));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.1f", this.ri));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.2e", this.c));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.1f", this.RMSE));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.2f", this.RSQ));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.3f", this.alpha));
        sb.append(",");
        sb.append(String.format(Locale.US, "%.2f", this.fPeak));

        return sb;
    }*/

    private TaubinSolution(Parcel in) {
        rNaught = in.readDouble();
        rInfinity = in.readDouble();
        alpha = in.readDouble();
        re = in.readDouble();
        ri = in.readDouble();
        tau = in.readDouble();
        fPeak = in.readDouble();
        c = in.readDouble();
        RMSE = in.readDouble();
        RSQ = in.readDouble();
        xOffset = in.readDouble();
        rOffset = in.readDouble();
        radius = in.readDouble();

        if (in.readByte() == 0x01) {
            measuredRXPairs = new ArrayList<>();
            in.readList(measuredRXPairs, RXpair.class.getClassLoader());
        }
        else {
            measuredRXPairs = null;
        }
        if (in.readByte() == 0x01) {
            fittedRXPairs = new ArrayList<>();
            in.readList(fittedRXPairs, RXpair.class.getClassLoader());
        }
        else {
            fittedRXPairs = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(rNaught);
        dest.writeDouble(rInfinity);
        dest.writeDouble(alpha);
        dest.writeDouble(re);
        dest.writeDouble(ri);
        dest.writeDouble(tau);
        dest.writeDouble(fPeak);
        dest.writeDouble(c);
        dest.writeDouble(RMSE);
        dest.writeDouble(RSQ);
        dest.writeDouble(xOffset);
        dest.writeDouble(rOffset);
        dest.writeDouble(radius);

        if (measuredRXPairs == null) {
            dest.writeByte((byte) (0x00));
        }
        else {
            dest.writeByte((byte) (0x01));
            dest.writeList(measuredRXPairs);
        }
        if (fittedRXPairs == null) {
            dest.writeByte((byte) (0x00));
        }
        else {
            dest.writeByte((byte) (0x01));
            dest.writeList(fittedRXPairs);
        }
    }

    public static final Parcelable.Creator<TaubinSolution> CREATOR = new Parcelable.Creator<TaubinSolution>() {
        @Override
        public TaubinSolution createFromParcel(Parcel in) {
            return new TaubinSolution(in);
        }

        @Override
        public TaubinSolution[] newArray(int size) {
            return new TaubinSolution[size];
        }
    };

    @Override
    public int size() {
        return measuredRXPairs.size();
    }

    @Override
    public Number getX(int index) {
        return measuredRXPairs.get(index).resistance;
    }

    @Override
    public Number getY(int index) {
        return measuredRXPairs.get(index).reactance;
    }

    @Override
    public String getTitle() {
        return "";
    }
}
