package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

class Node {

    private IntVar predicate; // tokens first, then predicates
    private IntVar[] arguments; // variables first, then constants
    private IntVar arity;

    Node(Model model) {
        int numPossibleArguments = GeneratePrograms.VARIABLES.length + GeneratePrograms.CONSTANTS.length + 1;
        predicate = model.intVar(0, GeneratePrograms.PREDICATES.length + Token.values().length - 1);
        arguments = model.intVarArray(GeneratePrograms.MAX_ARITY, 0, numPossibleArguments - 1);

        // Restrict the number of arguments to the right arity.
        // This also takes care of nullifying the arguments of tokens
        arity = model.intVar(0, GeneratePrograms.MAX_ARITY);
        model.table(predicate, arity, GeneratePrograms.arities).post();
        for (int i = 0; i < arguments.length; i++) {
            // If i >= arity, then arguments[i] must be undefined
            Constraint iGTEQArity = model.arithm(arity, "<=", i);
            Constraint fixArgument = model.arithm(arguments[i], "=", numPossibleArguments - 1);
            Constraint notDisabled = model.arithm(arguments[i], "!=", numPossibleArguments - 1);
            model.ifThenElse(iGTEQArity, fixArgument, notDisabled);
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
        StringBuilder atom = new StringBuilder(GeneratePrograms.PREDICATES[value - Token.values().length]);
        atom.append("(");
        for (int i = 0; i < arity.getValue(); i++) {
            if (i > 0)
                atom.append(", ");
            int index = arguments[i].getValue();
            if (index < GeneratePrograms.VARIABLES.length) {
                atom.append(GeneratePrograms.VARIABLES[index]);
            } else {
                atom.append(GeneratePrograms.CONSTANTS[index - GeneratePrograms.VARIABLES.length]);
            }
        }
        atom.append(")");
        return atom.toString();
    }
}
