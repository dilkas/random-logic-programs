package model;

import propagators.Condition;

/** A pair of predicates. Used to define pairs of independent predicates. */
public class PredicatePair {

    private String predicate1;
    private String predicate2;
    private Condition condition;

    PredicatePair(String predicate1, String predicate2) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    PredicatePair(String predicate1, String predicate2, Condition condition) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
        this.condition = condition;
    }

    boolean isConditional() {
        return condition != null;
    }

    public String getFirst() {
        return predicate1;
    }

    public String getSecond() {
        return predicate2;
    }

    public Condition getCondition() {
        return condition;
    }

    public static int toInt(String[] predicates, String predicate) {
        for (int i = 0; i < predicates.length; i++)
            if (predicates[i].equals(predicate))
                return i;
        throw new IllegalArgumentException();
    }
}
