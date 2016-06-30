package com.github.blemale.fuse;

import java.util.Arrays;

public interface Condition {
    void update(CallStatus callStatus);

    void reset();

    boolean isTrue();

    class FailureCount implements Condition {
        private final int threshold;

        private int count;

        public FailureCount(int threshold) {
            Preconditions.requireArgument(threshold > 0, "threshold should be > 0");
            this.threshold = threshold;
        }

        @Override
        public void update(CallStatus callStatus) {
            int inc = callStatus == CallStatus.SUCCESS ? 0 : 1;
            count += inc;
        }

        @Override
        public void reset() {
            count = 0;
        }

        @Override
        public boolean isTrue() {
            return count < threshold;
        }
    }

    class FailureRate implements Condition {
        private final double threshold;
        private final int windowLength;
        private final int[] values;

        private int index = 0;
        private int count = 0;

        public FailureRate(double threshold, int windowLength) {
            Preconditions.requireArgument(threshold > 0, "threshold should be > 0");
            Preconditions.requireArgument(windowLength > 0, "threshold should be > 0");
            this.threshold = threshold;
            this.windowLength = windowLength;
            this.values = new int[windowLength];
        }

        @Override
        public void update(CallStatus callStatus) {
            int oldValue = values[index];
            int newValue = callStatus == CallStatus.SUCCESS ? 0 : 1;

            values[index] = newValue;
            index = (index + 1) % windowLength;

            count = count + newValue - oldValue;
        }

        @Override
        public void reset() {
            Arrays.fill(values, 0);
            index = 0;
            count = 0;
        }

        @Override
        public boolean isTrue() {
            return ((double) count) / windowLength < threshold;
        }
    }

}
