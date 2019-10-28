import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;

public class Program {

    private static final int NUM_SOLUTIONS = 10000;
    private static final int NUM_NODES = 10;
    private static final String[] VALUES = {"not", "and", "or", "T", "p(X)", "p(X)"};
    private static final int NUM_CONNECTIVES = 2;

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

        // Tree structure
        IntVar[] treeStructure = model.intVarArray("treeStructure", NUM_NODES, 0, NUM_NODES - 1);
        IntVar numTrees = model.intVar("numTrees", 1);
        model.tree(treeStructure, numTrees).post();

        // Tree values
        IntVar[] treeValues = model.intVarArray("treeValues", NUM_NODES, 0, VALUES.length - 1);
        model.sort(treeValues, treeValues).post();
        for (int i = 0; i < NUM_NODES; i++) {
            IntVar exactlyZero = model.intVar("exactlyZero" + i, 0);
            IntVar exactlyOne = model.intVar("exactlyOne" + i, 1);
            IntVar moreThanOne = model.intVar("moreThanOne" + i, 2, NUM_NODES);

            Constraint isLeaf = model.count(i, treeStructure, exactlyZero);
            Constraint isNegation = model.count(i, treeStructure, exactlyOne);
            Constraint isInternal = model.count(i, treeStructure, moreThanOne);
            Constraint mustBeAConnective = model.or(model.arithm(treeValues[i], "=", 1),
                    model.arithm(treeValues[i], "=", 2));

            model.ifOnlyIf(isLeaf, model.arithm(treeValues[i], ">", NUM_CONNECTIVES));
            model.ifOnlyIf(isNegation, model.arithm(treeValues[i], "=", 0));
            model.ifOnlyIf(isInternal, mustBeAConnective);

            // TODO: Unreachable values have to be zero
        }

        // Configure search strategy
        java.util.Random rng = new java.util.Random();
        Solver solver = model.getSolver();
        IntVar[] decisionVariables = new IntVar[2 * NUM_NODES];
        for (int i = 0; i < NUM_NODES; i++) {
            decisionVariables[i] = treeStructure[i];
            decisionVariables[NUM_NODES + i] = treeValues[i];
        }
        solver.setSearch(Search.intVarSearch(new Random<>(rng.nextLong()),
                new IntDomainRandom(rng.nextLong()), decisionVariables));

        // Print solutions
        for (int i = 0; i < NUM_SOLUTIONS && solver.solve(); i++) {
            //simplePrint(treeStructure);
            //simplePrint(treeValues);
            int j = 0;
            for (; j < NUM_NODES; j++)
                if (treeStructure[j].getValue() == j)
                    break;
            System.out.println(treeToString(treeStructure, treeValues, j));
        }
    }
}
