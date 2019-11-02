import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

public class GeneratePrograms {

    private static final int NUM_SOLUTIONS = 10000;
    private static final int MAX_NUM_NODES = 4;
    private static final String[] PREDICATES = {"p(X)", "q(X)", "r(X)", "s(X)"};
    private static final int MAX_NUM_CLAUSES = 5;
    private static final boolean FORBID_ALL_CYCLES = false;
    private static final double[] PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
            1, 1, 1, 1, 1, 1}; // let's make probability 1 a bit more likely

    private static Model model;
    private static IntVar[] clauseAssignments;
    private static Clause[] clauses;

    private static String clauseToString(int i, java.util.Random rng) {
        int predicate = clauseAssignments[i].getValue();
        if (predicate == PREDICATES.length)
            return "";

        // Add a probability to the statement
        int probability = rng.nextInt(PROBABILITIES.length);
        String probabilityString = "";
        if (PROBABILITIES[probability] < 1)
            probabilityString = PROBABILITIES[probability] + " :: ";

        String clause = clauses[i].toString();
        if (clause.equals("T."))
            return probabilityString + PREDICATES[predicate] + ".\n";
        return probabilityString + PREDICATES[predicate] + " :- " + clause + "\n";
    }

    public static void main(String[] args) throws IOException {
        model = new Model();

        // numbers < PREDICATES.length assign a clause to a predicate, PREDICATES.length is used to discard the clause
        clauseAssignments = model.intVarArray(MAX_NUM_CLAUSES, 0, PREDICATES.length);

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

        clauses = new Clause[MAX_NUM_CLAUSES];
        for (int i = 0; i < MAX_NUM_CLAUSES; i++)
            clauses[i] = new Clause(model, clauseAssignments[i], PREDICATES, MAX_NUM_NODES);
        new Constraint("NoNegativeCycles",
                new NegativeCyclePropagator(clauseAssignments, clauses, FORBID_ALL_CYCLES)).post();

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

        // Adding a graph representation
        IntVar[][] adjacencyMatrix = model.intVarMatrix(PREDICATES.length, PREDICATES.length, 0, 1);
        IntVar zero = model.intVar(0);
        for (int i = 0; i < PREDICATES.length; i++) {
            for (int j = 0; j < PREDICATES.length; j++) {
                Constraint noEdge = model.arithm(adjacencyMatrix[i][j], "=", 0);
                Constraint[] clausesAssignedToJHaveNoI = new Constraint[clauses.length];
                for (int k = 0; k < clauses.length; k++) {
                    Constraint notAssignedToJ = model.arithm(clauseAssignments[k], "!=", j);
                    Constraint hasNoI = model.count(i, clauses[k].getTreeValues(), zero);
                    clausesAssignedToJHaveNoI[k] = model.or(notAssignedToJ, hasNoI);
                }
                // A[i][j] = 0 iff there are no clauses such that clauseAssignments[k] = j
                // and i is in clause[k].treeValues
                model.ifOnlyIf(noEdge, model.and(clausesAssignedToJHaveNoI));
            }
        }

        // Add extra conditions. TODO: remove when no longer necessary
        model.arithm(clauseAssignments[0], "=", 0).post();
        IntVar[] treeStructure = clauses[0].getTreeStructure();
        IntVar[] treeValues = clauses[0].getTreeValues();
        model.arithm(treeValues[0], "=", 2).post();
        model.arithm(treeStructure[1], "=", 0).post();
        model.arithm(treeStructure[2], "=", 0).post();
        model.arithm(treeValues[1], "=", 5).post();
        model.arithm(treeValues[2], "=", 6).post();
        predicatesCannotMentionEachOther(1, 2);

        // Configure search strategy
        java.util.Random rng = new java.util.Random();
        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(new Random<>(rng.nextLong()),
                new IntDomainRandom(rng.nextLong()), decisionVariables));
        //solver.setRestartOnSolutions(); // takes much longer, but solutions are more random

        // Write generated programs to files
        for (int i = 0; i < NUM_SOLUTIONS && solver.solve(); i++) {
            StringBuilder program = new StringBuilder();
            for (int j = 0; j < MAX_NUM_CLAUSES; j++) {
                program.append(clauseToString(j, rng));
                System.out.println(clauses[j].simpleToString());
            }
            System.out.println("====================");

            BufferedWriter writer = new BufferedWriter(new FileWriter("../programs/" + i + ".pl"));
            writer.write(program.toString());
            writer.close();
        }
    }

    private static void predicatesCannotMentionEachOther(int... predicates) {
        int numConstantValues = Clause.countConstantValues();
        int[] predicateValues = new int[predicates.length];
        for (int i = 0; i < predicates.length; i++)
            predicateValues[i] = predicates[i] + numConstantValues;

        IntVar zero = model.intVar(0);
        for (int i = 0; i < clauseAssignments.length; i++) {
            Constraint headIsInP = model.member(clauseAssignments[i], predicates);
            Constraint bodyCannotBeInP = model.among(zero, clauses[i].getTreeValues(), predicateValues);
            model.ifThen(headIsInP, bodyCannotBeInP);
        }
    }
}
