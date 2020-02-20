package propagators;

import model.Token;

import java.util.List;

/** Used to represent the condition part of a conditional independence expression */
public class Condition {

    public String operator;
    public List<String> predicates;

    // Needed for Jackson
    public Condition() {}

    public Condition(String operator, List<String> predicates) {
        this.operator = operator;
        this.predicates = predicates;
    }

    Token getOperator() {
        if (operator.equals("AND"))
            return Token.AND;
         if (operator.equals("OR"))
            return Token.OR;
        throw new IllegalArgumentException("the operator must be either AND or OR");
    }
}
