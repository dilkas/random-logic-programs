import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

public class Clause {

    private static final String[] CONSTANT_VALUES = {"not", "and", "or", "T"};
    private static final int NUM_CONNECTIVES = 2; // AND and OR
    private static final int INDEX_OF_TRUE = 3;

    private int maxNumNodes;
    private String[] values;
    private IntVar[][] tree;
    private IntVar[] treeStructure;
    private IntVar[] treeValues;

    IntVar[] getDecisionVariables() {
        return ArrayUtils.flatten(tree);
    }

    private String treeToString(IntVar[] treeStructure, IntVar[] treeValues, int i) {
        int value = treeValues[i].getValue();
        if (value > NUM_CONNECTIVES)
            return values[value];
        if (value == 0) {
            int j = 0;
            for (; j < maxNumNodes; j++)
                if (j != i && treeStructure[j].getValue() == i)
                    break;
            return "not(" + treeToString(treeStructure, treeValues, j) + ")";
        }

        boolean first = true;
        StringBuilder output = new StringBuilder();
        for (int j = 0; j < maxNumNodes; j++) {
            if (j != i && treeStructure[j].getValue() == i) {
                if (first) {
                    first = false;
                } else {
                    output.append(" ").append(values[value]).append(" ");
                }
                output.append("(").append(treeToString(treeStructure, treeValues, j)).append(")");
            }
        }
        return output.toString();
    }

    private String simplePrint(IntVar[] array) {
        StringBuilder builder = new StringBuilder();
        for (IntVar i : array)
            builder.append(i.getValue()).append(" ");
        builder.append("\n");
        return builder.toString();
    }

    public String toString() {
        return treeToString(treeStructure, treeValues, 0);
    }

    Clause(Model model, IntVar assignment, String[] predicates, int maxNumNodes) {
        this.maxNumNodes = maxNumNodes;
        values = new String[CONSTANT_VALUES.length + predicates.length];
        System.arraycopy(CONSTANT_VALUES, 0, values, 0, CONSTANT_VALUES.length);
        System.arraycopy(predicates, 0, values, CONSTANT_VALUES.length, predicates.length);

        // First column determines the tree structure, second column assigns values to nodes
        IntVar numNodes = model.intVar(1, maxNumNodes);
        tree = model.intVarMatrix(maxNumNodes, 2, 0, Math.max(maxNumNodes - 1, values.length - 1));
        treeStructure = ArrayUtils.getColumn(tree, 0);
        treeValues = ArrayUtils.getColumn(tree, 1);

        // Tree structure
        IntVar numTrees = model.intVar(1, maxNumNodes);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post();

        model.arithm(numTrees, "+", numNodes, "=", maxNumNodes + 1).post();

        // Removing symmetries
        if (maxNumNodes > 1) {
            IntVar[][] treeWithoutRoot = Arrays.copyOfRange(tree, 1, maxNumNodes);
            model.keySort(treeWithoutRoot, null, treeWithoutRoot, 2).post();
        }

        for (int i = 0; i < maxNumNodes; i++) {
            Constraint outsideScope = model.arithm(numNodes, "<=", i);
            Constraint mustBeALoop = model.arithm(treeStructure[i], "=", i);
            Constraint valueIsZero = model.arithm(treeValues[i], "=", INDEX_OF_TRUE);
            Constraint isRestricted = model.arithm(treeStructure[i], "<", numNodes);
            model.ifThenElse(outsideScope, model.and(mustBeALoop, valueIsZero), isRestricted);
        }

        // Tree values
        for (int i = 0; i < maxNumNodes; i++) {
            model.arithm(treeValues[i], "<", values.length).post();
            IntVar exactlyZero = model.intVar(0);
            IntVar exactlyOne = model.intVar(1);
            IntVar moreThanOne = model.intVar(2, Math.max(maxNumNodes, 2));

            IntVar[] structureWithoutI = new IntVar[maxNumNodes - 1];
            for (int j = 0; j < i; j++)
                structureWithoutI[j] = treeStructure[j];
            for (int j = i + 1; j < maxNumNodes; j++)
                structureWithoutI[j - 1] = treeStructure[j];

            Constraint isLeaf = model.count(i, structureWithoutI, exactlyZero);
            Constraint isNegation = model.count(i, structureWithoutI, exactlyOne);
            Constraint isInternal = model.count(i, structureWithoutI, moreThanOne);
            Constraint mustBeAConnective = model.or(model.arithm(treeValues[i], "=", 1),
                    model.arithm(treeValues[i], "=", 2));

            // Dividing nodes into predicate nodes, negation nodes, and connective nodes
            model.ifOnlyIf(isLeaf, model.arithm(treeValues[i], ">", NUM_CONNECTIVES));
            model.ifOnlyIf(isNegation, model.arithm(treeValues[i], "=", 0));
            model.ifOnlyIf(isInternal, mustBeAConnective);
        }

        // Disable the clause (restrict it to a unique value) if required
        Constraint shouldBeDisabled = model.arithm(assignment, "=", predicates.length);
        Constraint oneNode = model.arithm(numNodes, "=", 1);
        Constraint alwaysTrue = model.arithm(treeValues[0], "=", INDEX_OF_TRUE);
        model.ifThen(shouldBeDisabled, model.and(oneNode, alwaysTrue));
    }
}
