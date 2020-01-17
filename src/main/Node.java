package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

class Node {

    private IntVar predicate; // Tokens first, then predicates
    private IntVar[] arguments; // Variables first, then constants
    private IntVar arity;
    private Program program;

    Node(Program program, Model model, int clauseIndex, int position) {
        this.program = program;
        int numPossibleArguments = program.variables.length + program.constants.length + 1;
        predicate = model.intVar("nodePredicate[" + clauseIndex + "][" + position + "]", 0,
                program.predicates.length + Token.values().length - 1);
        arguments = model.intVarArray("nodeArguments[" + clauseIndex + "][" + position + "]", program.maxArity,
                0, numPossibleArguments - 1);

        // Restrict the number of arguments to the right arity.
        // This also takes care of nullifying the arguments of tokens
        arity = model.intVar("nodeArity[" + clauseIndex + "][" + position + "]", 0, program.maxArity);
        model.table(predicate, arity, program.aritiesTable).post();
        for (int i = 0; i < arguments.length; i++) {
            // If i >= arity, then arguments[i] must be undefined
            Constraint iGTEQArity = model.arithm(arity, "<=", i);
            Constraint fixArgument = model.arithm(arguments[i], "=", numPossibleArguments - 1);
            model.ifOnlyIf(iGTEQArity, fixArgument);
        }
    }

    IntVar getPredicate() {
        return predicate;
    }

    IntVar[] getDecisionVariables() {
        return ArrayUtils.concat(arguments, predicate);
    }

    IntVar[] getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        int value = predicate.getValue();
        if (value < Token.values().length)
            return Token.values()[value].toString();
        StringBuilder atom = new StringBuilder(program.predicates[value - Token.values().length]);
        if (arity.getValue() == 0)
            return atom.toString();

        atom.append("(");
        for (int i = 0; i < arity.getValue(); i++) {
            if (i > 0)
                atom.append(", ");
            int index = arguments[i].getValue();
            if (index < program.variables.length) {
                atom.append(program.variables[index]);
            } else {
                atom.append(program.constants[index - program.variables.length]);
            }
        }
        atom.append(")");
        return atom.toString();
    }
}
