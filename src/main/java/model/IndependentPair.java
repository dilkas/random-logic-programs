package model;

import propagators.Condition;

/** A pair of predicates. Used to define pairs of independent predicates. */
public class IndependentPair {

    public String predicate1;
    public String predicate2;
    public Condition condition;

    public IndependentPair(String predicate1, String predicate2) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    public IndependentPair(String predicate1, String predicate2, Condition condition) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
        this.condition = condition;
    }

    public boolean isConditional() {
        return condition != null;
    }

    public static int toInt(String[] predicates, String predicate) {
        for (int i = 0; i < predicates.length; i++)
            if (predicates[i].equals(predicate))
                return i;
        throw new IllegalArgumentException();
    }
}
