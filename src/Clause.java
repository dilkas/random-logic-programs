import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.LinkedList;
import java.util.List;

class Clause {

    private static final String[] CONSTANT_VALUES = {"not", "and", "or", "T"};
    private static final String[] PROBLOG_TOKENS = {"\\+", ",", ";"};
    private static final int NUM_CONNECTIVES = 2; // AND and OR
    private static final int INDEX_OF_TRUE = 3;

    private int maxNumNodes;
    private String[] values;
    private IntVar[] treeStructure;
    private IntVar[] treeValues;

    /** A list of predicates featured in this clause. The sign of each predicate denotes whether the predicate is
     * negated or not (after unfolding all the logical connectives). */
    List<SignedPredicate> getPredicates() {
        return getPredicates(0);
    }

    private List<SignedPredicate> getPredicates(int index) {
        int valueIndex = treeValues[index].getValue();
        List<SignedPredicate> predicates = new LinkedList<>();

        // If the node is T (true)
        if (valueIndex == INDEX_OF_TRUE)
            return predicates;

        // If the node is a predicate
        if (valueIndex >= CONSTANT_VALUES.length) {
            predicates.add(new SignedPredicate(valueIndex - CONSTANT_VALUES.length, Sign.POS));
            return predicates;
        }

        // If the node is a NOT
        if (valueIndex == 0) {
            int firstChild = findFirstChild(index);

            // This only happens if the tree constraint is unsatisfied but negative cycle constraint is propagated first
            if (firstChild >= maxNumNodes)
                return predicates;

            List<SignedPredicate> descendants = getPredicates(firstChild);
            for (SignedPredicate descendant : descendants)
                descendant.setNegative();
            return descendants;
        }

        // If the node is AND/OR
        for (int i = 0; i < maxNumNodes; i++) {
            if (i != index && treeStructure[i].getValue() == index)
                predicates.addAll(getPredicates(i));
        }
        return predicates;
    }

    private int findFirstChild(int parent) {
        int i = 0;
        for (; i < maxNumNodes; i++)
            if (i != parent && treeStructure[i].getValue() == parent)
                break;
        return i;
    }

    IntVar[] getDecisionVariables() {
        return ArrayUtils.concat(treeStructure, treeValues);
    }

    IntVar[] getTreeStructure() {
        return treeStructure;
    }

    IntVar[] getTreeValues() {
        return treeValues;
    }

    static int countConstantValues() {
        return CONSTANT_VALUES.length;
    }

    private String treeToString(int i) {
        int value = treeValues[i].getValue();
        if (value > NUM_CONNECTIVES)
            return values[value];
        if (value == 0)
            return PROBLOG_TOKENS[0] + "(" + treeToString(findFirstChild(i)) + ")";

        boolean first = true;
        StringBuilder output = new StringBuilder();
        for (int j = 0; j < maxNumNodes; j++) {
            if (j != i && treeStructure[j].getValue() == i) {
                if (first) {
                    first = false;
                } else {
                    output.append(PROBLOG_TOKENS[value]).append(" ");
                }
                output.append("(").append(treeToString(j)).append(")");
            }
        }
        return output.toString();
    }

    String simpleToString() {
        return "structure: " + arrayToString(treeStructure) + "values:    " + arrayToString(treeValues);
    }

    private static String arrayToString(IntVar[] array) {
        StringBuilder builder = new StringBuilder();
        for (IntVar i : array)
            builder.append(i.getValue()).append(" ");
        builder.append("\n");
        return builder.toString();
    }

    public String toString() {
        return treeToString(0) + ".";
    }

    Clause(Model model, IntVar assignment, String[] predicates, int maxNumNodes) {
        this.maxNumNodes = maxNumNodes;
        values = new String[CONSTANT_VALUES.length + predicates.length];
        System.arraycopy(CONSTANT_VALUES, 0, values, 0, CONSTANT_VALUES.length);
        System.arraycopy(predicates, 0, values, CONSTANT_VALUES.length, predicates.length);

        // First column determines the tree structure, second column assigns values to nodes
        IntVar numNodes = model.intVar(1, maxNumNodes);
        treeStructure = model.intVarArray(maxNumNodes, 0, maxNumNodes - 1);
        treeValues = model.intVarArray(maxNumNodes, 0, values.length - 1);

        // Tree structure
        IntVar numTrees = model.intVar(1, maxNumNodes);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post(); // the 0th element is always a root

        model.arithm(numTrees, "+", numNodes, "=", maxNumNodes + 1).post();
        model.sort(treeStructure, treeStructure).post();

        for (int i = 0; i < maxNumNodes; i++) {
            Constraint outsideScope = model.arithm(numNodes, "<=", i);
            Constraint mustBeALoop = model.arithm(treeStructure[i], "=", i);
            Constraint fixValue = model.arithm(treeValues[i], "=", INDEX_OF_TRUE);
            Constraint isRestricted = model.arithm(treeStructure[i], "<", numNodes);
            model.ifThenElse(outsideScope, model.and(mustBeALoop, fixValue), isRestricted);
        }

        // Tree values
        for (int i = 0; i < maxNumNodes; i++) {
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
            Constraint mustBeAConnective = model.member(treeValues[i], new int[]{1, 2});

            // Dividing nodes into predicate nodes, negation nodes, and connective nodes
            model.ifOnlyIf(isLeaf, model.arithm(treeValues[i], ">", NUM_CONNECTIVES));
            model.ifOnlyIf(isNegation, model.arithm(treeValues[i], "=", 0));
            model.ifOnlyIf(isInternal, mustBeAConnective);

            // 'True' (T) is only acceptable for root nodes
            if (i > 0) {
                Constraint notRoot = model.arithm(treeStructure[i], "!=", i);
                Constraint cannotBeTrue = model.arithm(treeValues[i], "!=", INDEX_OF_TRUE);
                model.ifThen(notRoot, cannotBeTrue);
            }
        }

        // Disable the clause (restrict it to a unique value) if required
        Constraint shouldBeDisabled = model.arithm(assignment, "=", predicates.length);
        Constraint oneNode = model.arithm(numNodes, "=", 1);
        Constraint alwaysTrue = model.arithm(treeValues[0], "=", INDEX_OF_TRUE);
        model.ifThen(shouldBeDisabled, model.and(oneNode, alwaysTrue));
    }
}
