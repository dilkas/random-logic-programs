package main;

/** A pair of predicates. Used to define pairs of independent predicates. */
class PredicatePair {

    private String[] predicates;
    private String predicate1;
    private String predicate2;

    PredicatePair(String[] predicates, String predicate1, String predicate2) {
        this.predicates = predicates;
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    int getFirst() {
        return get(predicate1);
    }

    int getSecond() {
        return get(predicate2);
    }

    private int get(String predicate) {
        for (int i = 0; i < predicates.length; i++)
            if (predicates[i].equals(predicate))
                return i;
        throw new IllegalArgumentException();
    }
}
