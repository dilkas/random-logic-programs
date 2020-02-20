package propagators;

import model.Token;

import java.util.List;

/** Used to represent the condition part of a conditional independence expression */
public class Condition {

    public String operator;
    public List<String> predicates;

    Token getOperator() {
        if (operator.equals("AND")) {
            return Token.AND;
        } else if (operator.equals("OR")) {
            return Token.OR;
        } else {
            throw new IllegalArgumentException("the operator must be either AND or OR");
        }
    }

    public Condition(String operator, List<String> predicates) {
        this.operator = operator;
        this.predicates = predicates;
    }
}
