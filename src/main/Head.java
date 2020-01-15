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
    private Program program;

    Head(Program program, Model model, IntVar predicate) {
        this.program = program;
        this.predicate = predicate;

        // Define arity
        IntVar indexToTable = model.intOffsetView(predicate, Token.values().length);
        arity = model.intVar(0, program.maxArity);
        model.table(indexToTable, arity, program.arities).post();

        // Arity regulates how many arguments the predicate has
        int numValues = program.constants.length + program.variables.length;
        arguments = model.intVarArray(program.maxArity, 0, numValues);
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
        s.append(program.predicates[predicate.getValue()]);
        if (arity.getValue() == 0)
            return s.toString();

        s.append("(");
        for (int i = 0; i < arity.getValue(); i++) {
            if (i > 0)
                s.append(", ");
            int argument = arguments[i].getValue();
            if (argument < program.variables.length) {
                s.append(program.variables[argument]);
            } else {
                s.append(program.constants[argument - program.variables.length]);
            }
        }
        s.append(")");
        return s.toString();
    }
}
