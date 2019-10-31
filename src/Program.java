import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

public class Program {

    private static final int NUM_SOLUTIONS = 10000;
    private static final int MAX_NUM_NODES = 5;
    private static final String[] PREDICATES = {"p(X)", "q(X)", "r(X)"};
    private static final int MAX_NUM_CLAUSES = 3;
    private static final double[] PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
            1, 1, 1, 1, 1, 1}; // let's make probability 1 a bit more likely

    public static void main(String[] args) {
        Model model = new Model();

        // numbers < PREDICATES.length assign a clause to a predicate, PREDICATES.length is used to discard the clause
        IntVar[] clauseAssignments = model.intVarArray(MAX_NUM_CLAUSES, 0, PREDICATES.length);

        model.sort(clauseAssignments, clauseAssignments).post();

        // Each possible value (< PREDICATES.length) should be mentioned at least once
        IntVar numDisabledClauses = model.intVar(0, MAX_NUM_CLAUSES - PREDICATES.length);
        model.count(PREDICATES.length, clauseAssignments, numDisabledClauses).post();
        IntVar numDistinctValues = model.intVar(PREDICATES.length, PREDICATES.length + 1);
        model.nValues(clauseAssignments, numDistinctValues).post();

        Constraint containsDisabledClause = model.arithm(numDisabledClauses, ">", 0);
        Constraint allValues = model.arithm(numDistinctValues, "=", PREDICATES.length + 1);
        Constraint allButOne = model.arithm(numDistinctValues, "=", PREDICATES.length);
        model.ifThenElse(containsDisabledClause, allValues, allButOne);

        Clause[] clauses = new Clause[MAX_NUM_CLAUSES];
        for (int i = 0; i < MAX_NUM_CLAUSES; i++)
            clauses[i] = new Clause(model, clauseAssignments[i], PREDICATES, MAX_NUM_NODES);

        // The order of the clauses doesn't matter (but we still allow duplicates)
        IntVar[] decisionVariables = clauseAssignments;
        IntVar[] previousDecisionVariables = null;
        for (int i = 0; i < MAX_NUM_CLAUSES; i++) {
            IntVar[] currentDecisionVariables = clauses[i].getDecisionVariables();
            decisionVariables = ArrayUtils.concat(decisionVariables, currentDecisionVariables);
            if (i > 0) {
                Constraint sameClause = model.arithm(clauseAssignments[i], "=", clauseAssignments[i-1]);
                Constraint lexOrdering = model.lexLessEq(previousDecisionVariables, currentDecisionVariables);
                model.ifThen(sameClause, lexOrdering);
            }
            previousDecisionVariables = currentDecisionVariables;
        }

        new Constraint("NoNegativeCycles", new NegativeCyclePropagator(clauseAssignments, clauses)).post();

        // Configure search strategy
        java.util.Random rng = new java.util.Random();
        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(new Random<>(rng.nextLong()),
                new IntDomainRandom(rng.nextLong()), decisionVariables));
        solver.setRestartOnSolutions();

        // Print solutions
        for (int i = 0; i < NUM_SOLUTIONS && solver.solve(); i++) {
            for (int j = 0; j < MAX_NUM_CLAUSES; j++) {
                int predicate = clauseAssignments[j].getValue();
                if (predicate == PREDICATES.length)
                    break;

                int probability = rng.nextInt(PROBABILITIES.length);
                String probabilityString = "";
                if (PROBABILITIES[probability] < 1)
                    probabilityString = PROBABILITIES[probability] + " :: ";

                String clause = clauses[j].toString();
                if (clause.equals("T.")) {
                    System.out.println(probabilityString + PREDICATES[predicate] + ".");
                } else {
                    System.out.println(probabilityString + PREDICATES[predicate] + " :- " + clause);
                }
            }
            System.out.println();
        }
    }
}
