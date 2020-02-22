package model;

import java.util.List;

/** Used to represent the condition part of a conditional independence expression and the 'required formula' part of
 * the config */
public class Formula {

    public String operator;
    public List<String> predicates;

    // Needed for Jackson
    public Formula() {}

    public Formula(String operator, List<String> predicates) {
        this.operator = operator;
        this.predicates = predicates;
    }

    public Token getOperator() {
        if (operator.equals("AND"))
            return Token.AND;
         if (operator.equals("OR"))
            return Token.OR;
        throw new IllegalArgumentException("the operator must be either AND or OR");
    }

    public int[] getPredicates(List<String> predicateNames) {
        int[] intPredicates = new int[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            for (int j = 0; j < predicateNames.size(); j++) {
                if (predicates.get(i).equals(predicateNames.get(j))) {
                    intPredicates[i] = j + Token.values().length;
                    break;
                }
            }
            if (intPredicates[i] == 0)
                throw new IllegalArgumentException("Non-existant predicate name: " + predicates.get(i));
        }
        return intPredicates;
    }
}
