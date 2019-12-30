package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

/** The head of a clause */
class Head {

    private IntVar predicate; // PREDICATES.length means that the clause is disabled
    private IntVar arity;
    private IntVar[] variables; // How many times each variable should be included as an argument
    // A map from positions to constants, where 0 means that either that position is disabled or occupied by a variable
    private IntVar[] constants;

    // Auxiliary variables
    private IntVar sumOfVars;
    private IntVar countVariables;
    private IntVar negRemainingArity;

    Head(Model model, IntVar predicate) {
        this.predicate = predicate;

        IntVar indexToTable = model.intOffsetView(predicate, Token.values().length);
        arity = model.intVar(0, GeneratePrograms.MAX_ARITY);
        model.table(indexToTable, arity, GeneratePrograms.arities).post();

        variables = model.intVarArray(GeneratePrograms.VARIABLES.length, 0, GeneratePrograms.MAX_ARITY);
        for (IntVar variable : variables)
            model.arithm(variable, "<=", arity).post();

        constants = model.intVarArray(GeneratePrograms.MAX_ARITY, 0, GeneratePrograms.CONSTANTS.length);
        for (int i = 0; i < constants.length; i++) {
            Constraint iGeArity = model.arithm(arity, "<=", i);
            Constraint isZero = model.arithm(constants[i], "=", 0);
            model.ifThen(iGeArity, isZero);
        }

        // Connecting the two arrays
        // countZeros - (MAX_ARITY - arity) = sumOfVars
        countVariables = model.intVar(0, constants.length);
        model.count(GeneratePrograms.CONSTANTS.length, constants, countVariables).post();
        sumOfVars = model.intVar(0, GeneratePrograms.MAX_ARITY);
        model.sum(variables, "=", sumOfVars).post();
        negRemainingArity = model.intVar(-GeneratePrograms.MAX_ARITY, 0);
        model.arithm(negRemainingArity, "=", arity, "-", GeneratePrograms.MAX_ARITY).post();
        model.arithm(sumOfVars, "=", countVariables, "+", negRemainingArity).post();

        // All zeros go after all non-zeros
        // v[i] = 0 /\ v[j] != 0 => j < i
        // j < i \/ v[i] != 0 \/ v[j] = 0
        // For j > i, v[i] != 0 \/ v[j] = 0
        for (int i = 0; i < variables.length - 1; i++) {
            for (int j = i + 1; j < variables.length; j++) {
                Constraint iNotZero = model.arithm(variables[i], "!=", 0);
                Constraint jIsZero = model.arithm(variables[j], "=", 0);
                model.or(iNotZero, jIsZero).post();
            }
        }
    }

    public IntVar[] getDecisionVariables() {
        return ArrayUtils.concat(variables, constants);
    }

    @Override
    public String toString() {
        /*System.out.println("Predicate: " + predicate.getValue());
        System.out.println("Arity: " + arity.getValue());
        System.out.print("Variables: ");
        for (IntVar variable : variables)
            System.out.print(variable.getValue() + " ");
        System.out.println();
        System.out.print("Constants: ");
        for (IntVar constant : constants)
            System.out.print(constant.getValue() + " ");
        System.out.println("\n");
        System.out.println("sum of variables: " + sumOfVars.getValue());
        System.out.println("number of variables: " + countVariables.getValue());
        System.out.println("negated remaining arity: " + negRemainingArity.getValue() + "\n");*/

        StringBuilder s = new StringBuilder();
        s.append(GeneratePrograms.PREDICATES[predicate.getValue()]).append("(");
        int variableIndex = 0; // Which variable to pick next?
        int variablesBeenUsed = 0; // How many times have we used this variable already?
        for (int i = 0; i < arity.getValue(); i++) {
            int argument = constants[i].getValue();
            if (argument < GeneratePrograms.CONSTANTS.length) {
                s.append(GeneratePrograms.CONSTANTS[argument]);
            } else {
                while (variables[variableIndex].getValue() <= variablesBeenUsed) {
                    variablesBeenUsed = 0;
                    variableIndex++;
                }
                variablesBeenUsed++;
                if (i > 0)
                    s.append(", ");
                s.append(GeneratePrograms.VARIABLES[variableIndex]);
            }
        }
        s.append(")");
        return s.toString();
    }
}
