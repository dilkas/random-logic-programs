class PotentialMask {
    private MaskValue mask;

    PotentialMask(MaskValue mask) {
        this.mask = mask;
    }

    MaskValue getMask() {
        return mask;
    }

    void upgradeTo(MaskValue newValue) {
        if (mask.ordinal() < newValue.ordinal())
            mask = newValue;
    }
}
