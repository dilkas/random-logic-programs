package propagators;

import model.Token;

import java.util.List;

/** Used to represent the condition part of a conditional independence expression */
public class Condition {

    private Token operator;
    private List<String> predicates;

    Token getOperator() {
        return operator;
    }

    List<String> getPredicates() {
        return predicates;
    }

    public Condition(String operator, List<String> predicates) {
        if (operator.equals("AND")) {
            this.operator = Token.AND;
        } else if (operator.equals("OR")) {
            this.operator = Token.OR;
        } else {
            throw new IllegalArgumentException("the operator must be either AND or OR");
        }
        this.operator = Token.valueOf(operator);
        this.predicates = predicates;
    }
}
