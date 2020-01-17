package propagators;

public class SignedPredicate {

    private int index;
    private Sign sign;

    public SignedPredicate(int index, Sign sign) {
        this.index = index;
        this.sign = sign;
    }

    int getIndex() {
        return index;
    }

    Sign getSign() {
        return sign;
    }

    /** We can only go from positive to negative, but not vice-versa */
    public void setNegative() {
        sign = Sign.NEG;
    }
}
