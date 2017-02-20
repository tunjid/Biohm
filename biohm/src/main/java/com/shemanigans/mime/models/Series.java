package com.shemanigans.mime.models;

import com.androidplot.xy.XYSeries;
import com.shemanigans.mime.services.BluetoothLeService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for plotting
 */
public class Series {

    public static class RXSeries implements XYSeries {
        private List<RXpair> rxPairs;

        public RXSeries() {
            rxPairs = new ArrayList<>(100);
        }

        public void updateRxPairs(List<RXpair> rxPairs) {
            this.rxPairs = rxPairs;
            //this.rxPairs.addAll(rxPairs);
        }

        @Override
        public int size() {
            return rxPairs.size();
        }

        @Override
        public Number getX(int index) {
            return rxPairs.get(index).resistance;
        }

        @Override
        public Number getY(int index) {
            return rxPairs.get(index).reactance;
        }

        @Override
        public String getTitle() {
            return "";
        }
    }

    public static class GenericSeries implements XYSeries {

        private final String title;
        private final FixedCircularQueue<Number> data;

        public GenericSeries(String title) {
            this.title = title;
            data = new FixedCircularQueue<>(Number.class, BluetoothLeService.HISTORY_SIZE);
        }

        public void addData(Number dataPoint) {
            data.add(dataPoint);
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data.get(index);
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private static class FixedCircularQueue<E> {

        private final int size;
        private final E[] data;

        private boolean isOverwritten = false;
        private int cursor = -1;
        private int firstItem = 0;

        @SuppressWarnings("unchecked")
        FixedCircularQueue(Class<E> clazz, int size) {
            this.size = size;
            // Use Array native method to create array
            // of a type only known at run time
            data = (E[]) Array.newInstance(clazz, size);
        }

        public void add(E object) {
            if (cursor >= size - 1) {
                isOverwritten = true;
                cursor = -1;
            }

            data[++cursor] = object;

            if (isOverwritten) {
                firstItem = cursor + 1;
            }
        }

        E get(int index) {
            int check = firstItem + index;

            int finalIndex = check >= size ? check - size : check;

            return data[finalIndex];
        }

        int size() {
            return isOverwritten ? size : cursor + 1;
        }

    }


}
