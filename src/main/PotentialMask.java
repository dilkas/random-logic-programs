package main;

import main.Possibility;

class PotentialMask {
    private Possibility mask;

    PotentialMask(Possibility mask) {
        this.mask = mask;
    }

    Possibility getMask() {
        return mask;
    }

    void upgradeTo(Possibility newValue) {
        if (mask.ordinal() < newValue.ordinal())
            mask = newValue;
    }
}
