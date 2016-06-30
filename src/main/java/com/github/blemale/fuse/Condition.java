package com.github.blemale.fuse;

public interface Condition {
    void update(CallStatus callStatus);
    void reset();
    boolean isTrue();

    class FailureCount implements Condition {
        private final int threshold;
        private int count;

        public FailureCount(int threshold) {
            this.threshold = threshold;
        }

        @Override
        public void update(CallStatus callStatus) {
            switch (callStatus) {
                case SUCCESS:
                    count = 0; break;
                case FAILURE:
                    count += 1; break;
                case OPEN:
                    count += 1; break;
            }
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

}
