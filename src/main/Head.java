package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

/** The head of a clause */
class Head {

    private IntVar predicate; // PREDICATES.length means that the clause is disabled
    private IntVar[] arguments;
    private IntVar arity;

    Head(Model model, IntVar predicate) {
        this.predicate = predicate;

        // Define arity
        IntVar indexToTable = model.intOffsetView(predicate, Token.values().length);
        arity = model.intVar(0, GeneratePrograms.MAX_ARITY);
        model.table(indexToTable, arity, GeneratePrograms.arities).post();

        // Arity regulates how many arguments the predicate has
        int numValues = GeneratePrograms.CONSTANTS.length + GeneratePrograms.VARIABLES.length;
        arguments = model.intVarArray(GeneratePrograms.MAX_ARITY, 0, numValues);
        for (int i = 0; i < arguments.length; i++) {
            Constraint iGeArity = model.arithm(arity, "<=", i);
            Constraint isDisabled = model.arithm(arguments[i], "=", numValues);
            model.ifOnlyIf(iGeArity, isDisabled);
        }

    }

    IntVar[] getArguments() {
        return arguments;
    }

    public IntVar[] getDecisionVariables() {
        return ArrayUtils.concat(new IntVar[]{predicate}, arguments);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(GeneratePrograms.PREDICATES[predicate.getValue()]).append("(");
        for (int i = 0; i < arity.getValue(); i++) {
            if (i > 0)
                s.append(", ");
            int argument = arguments[i].getValue();
            if (argument < GeneratePrograms.VARIABLES.length) {
                s.append(GeneratePrograms.VARIABLES[argument]);
            } else {
                s.append(GeneratePrograms.CONSTANTS[argument - GeneratePrograms.VARIABLES.length]);
            }
        }
        s.append(")");
        return s.toString();
    }
}
