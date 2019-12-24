class PotentialValue {

    private int value;
    private boolean determined;

    PotentialValue(int value, boolean determined) {
        this.value = value;
        this.determined = determined;
    }

    boolean isDetermined() {
        return determined;
    }

    public int getValue() {
        return value;
    }

}
