class SignedPredicate {

    private int index;
    private Sign sign;

    SignedPredicate(int index, Sign sign) {
        this.index = index;
        this.sign = sign;
    }

    int getIndex() {
        return index;
    }

    Sign getSign() {
        return sign;
    }

    void changeSign() {
        if (sign == Sign.POS) {
            sign = Sign.NEG;
        } else {
            sign = Sign.POS;
        }
    }
}
