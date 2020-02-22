package model;

import java.util.List;

/** A pair of predicates. Used to define pairs of independent predicates. */
public class IndependentPair {

    public String predicate1;
    public String predicate2;
    public Formula condition;

    // Needed for Jackson
    public IndependentPair() {}

    public IndependentPair(String predicate1, String predicate2) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    public IndependentPair(String predicate1, String predicate2, Formula condition) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
        this.condition = condition;
    }

    public boolean isConditional() {
        return condition != null;
    }

    public static int toInt(List<String> predicates, String predicate) {
        for (int i = 0; i < predicates.size(); i++)
            if (predicates.get(i).equals(predicate))
                return i;
        throw new IllegalArgumentException();
    }
}
