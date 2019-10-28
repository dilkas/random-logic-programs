import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

public class Program {

    private static final int NUM_SOLUTIONS = 10000;
    private static final int NUM_NODES = 10;
    private static final String[] VALUES = {"not", "and", "or", "T", "p(X)"};
    private static final int NUM_CONNECTIVES = 2; // AND and OR

    private static String treeToString(IntVar[] treeStructure, IntVar[] treeValues, int i) {
        int value = treeValues[i].getValue();
        if (value > NUM_CONNECTIVES)
            return VALUES[value];
        if (value == 0) {
            int j = 0;
            for (; j < NUM_NODES; j++)
                if (j != i && treeStructure[j].getValue() == i)
                    break;
            return "not(" + treeToString(treeStructure, treeValues, j) + ")";
        }

        boolean first = true;
        StringBuilder output = new StringBuilder();
        for (int j = 0; j < NUM_NODES; j++) {
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
        IntVar[][] tree = model.intVarMatrix(NUM_NODES, 2, 0, Math.max(NUM_NODES - 1, VALUES.length - 1));
        IntVar[] treeStructure = ArrayUtils.getColumn(tree, 0);
        IntVar[] treeValues = ArrayUtils.getColumn(tree, 1);

        // Removing symmetries
        if (NUM_NODES > 1) {
            IntVar[][] treeWithoutRoot = Arrays.copyOfRange(tree, 1, NUM_NODES);
            model.keySort(treeWithoutRoot, null, treeWithoutRoot, 2).post();
        }

        // The two columns should have different upper bounds
        int column, upper_bound;
        if (NUM_NODES > VALUES.length) {
            column = 1;
            upper_bound = VALUES.length;
        } else {
            column = 0;
            upper_bound = NUM_NODES;
        }
        for (int i = 0; i < NUM_NODES; i++)
            model.arithm(tree[i][column], "<", upper_bound).post();

        // Tree structure
        IntVar numTrees = model.intVar("numTrees", 1);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post();

        // Tree values
        for (int i = 0; i < NUM_NODES; i++) {
            IntVar exactlyZero = model.intVar("exactlyZero" + i, 0);
            IntVar exactlyOne = model.intVar("exactlyOne" + i, 1);
            IntVar moreThanOne = model.intVar("moreThanOne" + i, 2, Math.max(NUM_NODES, 2));

            IntVar[] structureWithoutI = new IntVar[NUM_NODES - 1];
            for (int j = 0; j < i; j++)
                structureWithoutI[j] = treeStructure[j];
            for (int j = i + 1; j < NUM_NODES; j++)
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
            //simplePrint(treeStructure);
            //simplePrint(treeValues);
            System.out.println(treeToString(treeStructure, treeValues, 0));
        }
    }
}
