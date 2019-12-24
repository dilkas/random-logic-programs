package main;

public enum Possibility {
    NO, MAYBE, YES;

    Possibility upgradeTo(Possibility value) {
        if (ordinal() < value.ordinal())
            return value;
        return this;
    }
}
