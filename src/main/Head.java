package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;

/** The head of a clause */
class Head {

    private IntVar predicate; // PREDICATES.length means that the clause is disabled
    private IntVar[] arguments;
    private IntVar arity;
    private SetVar[] occurrences;

    Head(Model model, IntVar predicate) {
        this.predicate = predicate;

        // Define arity
        IntVar indexToTable = model.intOffsetView(predicate, Token.values().length);
        arity = model.intVar(0, GeneratePrograms.MAX_ARITY);
        model.table(indexToTable, arity, GeneratePrograms.arities).post();

        // Arity regulates how many arguments the predicate has
        arguments = model.intVarArray(GeneratePrograms.MAX_ARITY, 0,
                GeneratePrograms.CONSTANTS.length + GeneratePrograms.VARIABLES.length - 1);
        for (int i = 0; i < arguments.length; i++) {
            Constraint iGeArity = model.arithm(arity, "<=", i);
            Constraint isDisabled = model.arithm(arguments[i], "=",
                    GeneratePrograms.CONSTANTS.length + GeneratePrograms.VARIABLES.length - 1);
            model.ifThen(iGeArity, isDisabled);
        }

        int[] possibleIndices = new int[GeneratePrograms.MAX_ARITY];
        for (int i = 0; i < GeneratePrograms.MAX_ARITY; i++)
            possibleIndices[i] = i;
        // For each variable, we store the set of indices where it occurs
        occurrences = model.setVarArray(GeneratePrograms.CONSTANTS.length +
                GeneratePrograms.VARIABLES.length, new int[0], possibleIndices);
        model.setsIntsChanneling(occurrences, arguments).post();
    }

    SetVar[] getOccurrences() {
        return occurrences;
    }

    public IntVar[] getDecisionVariables() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(GeneratePrograms.PREDICATES[predicate.getValue()]).append("(");
        for (int i = 0; i < arity.getValue(); i++) {
            if (i > 0)
                s.append(", ");
            int argument = arguments[i].getValue();
            if (argument < GeneratePrograms.CONSTANTS.length) {
                s.append(GeneratePrograms.CONSTANTS[argument]);
            } else {
                s.append(GeneratePrograms.VARIABLES[argument - GeneratePrograms.CONSTANTS.length]);
            }
        }
        s.append(")");
        return s.toString();
    }
}
