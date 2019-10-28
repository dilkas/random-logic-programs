import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

public class Clause {

    private static final int NUM_SOLUTIONS = 10000;
    private static final int MAX_NUM_NODES = 5;
    private static final String[] VALUES = {"not", "and", "or", "T", "p(X)"};
    private static final int NUM_CONNECTIVES = 2; // AND and OR
    private static final int INDEX_OF_TRUE = 3;

    private static String treeToString(IntVar[] treeStructure, IntVar[] treeValues, int i) {
        int value = treeValues[i].getValue();
        if (value > NUM_CONNECTIVES)
            return VALUES[value];
        if (value == 0) {
            int j = 0;
            for (; j < MAX_NUM_NODES; j++)
                if (j != i && treeStructure[j].getValue() == i)
                    break;
            return "not(" + treeToString(treeStructure, treeValues, j) + ")";
        }

        boolean first = true;
        StringBuilder output = new StringBuilder();
        for (int j = 0; j < MAX_NUM_NODES; j++) {
            if (j != i && treeStructure[j].getValue() == i) {
                if (first) {
                    first = false;
                } else {
                    output.append(" ").append(VALUES[value]).append(" ");
                }
                output.append("(").append(treeToString(treeStructure, treeValues, j)).append(")");
            }
        }
        return output.toString();
    }

    private static void simplePrint(IntVar[] array) {
        for (IntVar i : array)
            System.out.print(i.getValue() + " ");
        System.out.println();
    }

    public static void main(String[] args) {
        Model model = new Model();

        // First column determines the tree structure, second column assigns values to nodes
        IntVar numNodes = model.intVar("numNodes", 1, MAX_NUM_NODES);
        IntVar[][] tree = model.intVarMatrix(MAX_NUM_NODES, 2, 0, Math.max(MAX_NUM_NODES - 1, VALUES.length - 1));
        IntVar[] treeStructure = ArrayUtils.getColumn(tree, 0);
        IntVar[] treeValues = ArrayUtils.getColumn(tree, 1);

        // Tree structure
        IntVar numTrees = model.intVar("numTrees", 1, MAX_NUM_NODES);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post();

        model.arithm(numTrees, "+", numNodes, "=", MAX_NUM_NODES + 1).post();

        // Removing symmetries
        if (MAX_NUM_NODES > 1) {
            IntVar[][] treeWithoutRoot = Arrays.copyOfRange(tree, 1, MAX_NUM_NODES);
            model.keySort(treeWithoutRoot, null, treeWithoutRoot, 2).post();
        }

        for (int i = 0; i < MAX_NUM_NODES; i++) {
            Constraint outsideScope = model.arithm(numNodes, "<=", i);
            Constraint mustBeALoop = model.arithm(treeStructure[i], "=", i);
            Constraint valueIsZero = model.arithm(treeValues[i], "=", INDEX_OF_TRUE);
            Constraint isRestricted = model.arithm(treeStructure[i], "<", numNodes);
            model.ifThenElse(outsideScope, model.and(mustBeALoop, valueIsZero), isRestricted);
        }

        // Tree values
        for (int i = 0; i < MAX_NUM_NODES; i++) {
            model.arithm(treeValues[i], "<", VALUES.length).post();
            IntVar exactlyZero = model.intVar("exactlyZero" + i, 0);
            IntVar exactlyOne = model.intVar("exactlyOne" + i, 1);
            IntVar moreThanOne = model.intVar("moreThanOne" + i, 2, Math.max(MAX_NUM_NODES, 2));

            IntVar[] structureWithoutI = new IntVar[MAX_NUM_NODES - 1];
            for (int j = 0; j < i; j++)
                structureWithoutI[j] = treeStructure[j];
            for (int j = i + 1; j < MAX_NUM_NODES; j++)
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

        // Configure search strategy
        java.util.Random rng = new java.util.Random();
        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(new Random<>(rng.nextLong()),
                new IntDomainRandom(rng.nextLong()), ArrayUtils.flatten(tree)));

        // Print solutions
        for (int i = 0; i < NUM_SOLUTIONS && solver.solve(); i++) {
            System.out.println("num nodes: " + numNodes.getValue());
            System.out.println("num trees: " + numTrees.getValue());
            simplePrint(treeStructure);
            simplePrint(treeValues);
            System.out.println(treeToString(treeStructure, treeValues, 0));
        }
    }
}
