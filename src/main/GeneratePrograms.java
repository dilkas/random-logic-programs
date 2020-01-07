package main;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.NegativeCyclePropagator;

import static java.util.stream.Collectors.toList;

class GeneratePrograms {

    private static final String DIRECTORY = "../programs/";
    private static final int NUM_SOLUTIONS = 10000;
    private static int MAX_NUM_NODES = 1;
    private static int MAX_NUM_CLAUSES = 1;
    private static final boolean FORBID_ALL_CYCLES = false;
    //private static final double[] PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
    //        1, 1, 1, 1, 1, 1}; // let's make probability 1 a bit more likely
    private static final double[] PROBABILITIES = {1};

    static String[] PREDICATES = {"p"};
    static int[] ARITIES = {1};
    static String[] VARIABLES = {"X", "Y", "Z"};
    static String[] CONSTANTS = {"a"};
    static int MAX_ARITY = Arrays.stream(ARITIES).max().getAsInt();

    static Tuples arities;

    private static Model model;
    private static IntVar[] clauseAssignments; // an array of predicates occurring at the heads of clauses
    private static Head[] clauseHeads; // full heads (predicates, variables, constants)
    private static Body[] bodies; // the body of each clause
    private static IntVar[] decisionVariables; // all decision variables relevant to the problem
    private static IntVar[][] termsPerClause; // a flattened-out view of the term positions in each clause
    private static SetVar[][] occurrences; // for each clause and variable, a set of positions with that variable
    private static IntVar[][] M; // for eliminating variable symmetries
    private static java.util.Random rng;

    /** The entire clause, i.e., both body and head */
    private static String clauseToString(int i, java.util.Random rng) {
        for (int j = 0; j < termsPerClause[i].length; j++)
            System.out.print(termsPerClause[i][j].getValue() + " ");
        System.out.println();
        for (int j = 0; j < occurrences[i].length; j++)
            System.out.print(occurrences[i][j].getValue() + " ");
        System.out.println();
        for (int j = 0; j < M[i].length; j++)
            System.out.print(M[i][j].getValue() + " ");
        System.out.println();

        /*System.out.print("Occurrences of variables in the body: ");
        SetVar[] bodyOccurrences = bodies[i].getOccurrences();
        for (int j = 0; j < bodyOccurrences.length; j++)
            System.out.print(bodyOccurrences[j].getValue() + " ");
        System.out.println();*/

        // Is this clause disabled?
        int predicate = clauseAssignments[i].getValue();
        if (predicate == PREDICATES.length)
            return "";

        // Add a probability to the statement
        int probability = rng.nextInt(PROBABILITIES.length);
        String probabilityString = "";
        if (PROBABILITIES[probability] < 1)
            probabilityString = PROBABILITIES[probability] + " :: ";

        String body = bodies[i].toString();
        String head = clauseHeads[i].toString();
        if (body.equals("T."))
            return probabilityString + head+ ".\n";
        return probabilityString + head + " :- " + body + "\n";
    }

    /** Set up all the variables and constraints. */
    private static void setUp() {
        assert(PREDICATES.length == ARITIES.length);
        arities = new Tuples();
        for (Token t : Token.values())
            arities.add(t.ordinal(), 0); // Tokens don't have arities
        for (int i = 0; i < ARITIES.length; i++)
            arities.add(Token.values().length + i, ARITIES[i]); // Predicate arities are predefined
        arities.add(PREDICATES.length + Token.values().length, 0); // This stands for a disabled clause

        model = new Model();
        rng = new java.util.Random();

        // numbers < PREDICATES.length assign a clause to a predicate, PREDICATES.length is used to discard the clause
        clauseAssignments = model.intVarArray(MAX_NUM_CLAUSES, 0, PREDICATES.length);
        model.sort(clauseAssignments, clauseAssignments).post();

        clauseHeads = new Head[clauseAssignments.length];
        for (int i = 0; i < clauseHeads.length; i++)
            clauseHeads[i] = new Head(model, clauseAssignments[i]);

        IntVar numDisabledClauses = model.intVar(0, MAX_NUM_CLAUSES - PREDICATES.length);
        model.count(PREDICATES.length, clauseAssignments, numDisabledClauses).post();

        // Each possible value (< PREDICATES.length) should be mentioned at least once
        IntVar numDistinctValues = model.intVar(PREDICATES.length, PREDICATES.length + 1);
        model.nValues(clauseAssignments, numDistinctValues).post();

        Constraint containsDisabledClause = model.arithm(numDisabledClauses, ">", 0);
        Constraint allValues = model.arithm(numDistinctValues, "=", PREDICATES.length + 1);
        Constraint allButOne = model.arithm(numDistinctValues, "=", PREDICATES.length);
        model.ifThenElse(containsDisabledClause, allValues, allButOne);

        bodies = new Body[MAX_NUM_CLAUSES];
        for (int i = 0; i < MAX_NUM_CLAUSES; i++)
            bodies[i] = new Body(model, clauseAssignments[i], MAX_NUM_NODES);

        // The order of the clauses doesn't matter (but we still allow duplicates)
        decisionVariables = clauseAssignments;
        IntVar[] previousDecisionVariables = null;
        for (int i = 0; i < MAX_NUM_CLAUSES; i++) {
            IntVar[] currentDecisionVariables = bodies[i].getDecisionVariables();
            decisionVariables = ArrayUtils.concat(decisionVariables, currentDecisionVariables);
            decisionVariables = ArrayUtils.concat(decisionVariables, clauseHeads[i].getDecisionVariables());
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
                Constraint[] clausesAssignedToJHaveNoI = new Constraint[bodies.length];
                for (int k = 0; k < bodies.length; k++) {
                    Constraint notAssignedToJ = model.arithm(clauseAssignments[k], "!=", j);
                    Constraint hasNoI = model.count(i + Token.values().length,
                            bodies[k].getTreeValues(), zero);
                    clausesAssignedToJHaveNoI[k] = model.or(notAssignedToJ, hasNoI);
                }
                // A[i][j] = 0 iff there are no clauses such that clauseAssignments[k] = j
                // and i is in clause[k].treeValues
                model.ifOnlyIf(noEdge, model.and(clausesAssignedToJHaveNoI));
            }
        }

        // Set up termsPerClause to keep track of all variables and constants across each clause
        int numIndices = (MAX_NUM_NODES + 1) * MAX_ARITY;
        // a concatenation of all terms in each clause (both head and body) (dimension 1 - clauses, dimension 2 - positions)
        termsPerClause = new IntVar[MAX_NUM_CLAUSES][numIndices];
        for (int i = 0; i < MAX_NUM_CLAUSES; i++) {
            System.arraycopy(clauseHeads[i].getArguments(), 0, termsPerClause[i], 0, MAX_ARITY);
            System.arraycopy(bodies[i].getArguments(), 0, termsPerClause[i], MAX_ARITY, MAX_NUM_NODES * MAX_ARITY);
        }

        // Set up occurrences
        int maxValue = CONSTANTS.length + VARIABLES.length;
        int[] possibleIndices = new int[numIndices];
        for (int i = 0; i < numIndices; i++)
            possibleIndices[i] = i;
        occurrences = model.setVarMatrix(MAX_NUM_CLAUSES, maxValue + 1, new int[0], possibleIndices);
        for (int i = 0; i < MAX_NUM_CLAUSES; i++)
            model.setsIntsChanneling(occurrences[i], termsPerClause[i]).post();

        // Set up M
        M = new IntVar[MAX_NUM_CLAUSES][];
        for (int i = 0; i < MAX_NUM_CLAUSES; i++) {
            M[i] = model.intVarArray(VARIABLES.length, 0, numIndices);
            for (int v = 0; v < VARIABLES.length; v++) {
                model.min(occurrences[i][v], M[i][v], false).post();
                SetVar[] occurrencesAtI = new SetVar[1];
                occurrencesAtI[0] = occurrences[i][v];
                Constraint noOccurrences = model.nbEmpty(occurrencesAtI, 1);
                Constraint fixMinOccurrence = model.arithm(M[i][v], "=", numIndices);
                model.ifThen(noOccurrences, fixMinOccurrence);
            }
            model.sort(M[i], M[i]).post();
        }
    }

    private static void setUpExtraConditions() {
        new Constraint("NoNegativeCycles",
                new NegativeCyclePropagator(clauseAssignments, bodies, FORBID_ALL_CYCLES)).post();

        /*model.arithm(clauseAssignments[0], "=", 0).post();
        IntVar[] treeStructure = bodies[0].getTreeStructure();
        IntVar[] treeValues = bodies[0].getTreeValues();
        model.arithm(treeValues[0], "=", 1).post();
        model.arithm(treeStructure[1], "=", 0).post();
        model.arithm(treeStructure[2], "=", 0).post();
        model.arithm(treeValues[1], "=", 5).post();
        model.arithm(treeValues[2], "=", 6).post();
        new Constraint("q and r are independent",
                new IndependencePropagator(adjacencyMatrix, 1, 2)).post();
        List<Integer> predicates = new LinkedList<>();
        predicates.add(1);
        predicates.add(2);
        Mask qAndR = new Mask(Token.AND, predicates);
        new Constraint("p is independent of q given q and r", new IndependencePropagator(adjacencyMatrix,
                clauseAssignments, bodies, 0, 1, qAndR)).post();
        new Constraint("p is independent of r given q and r", new IndependencePropagator(adjacencyMatrix,
                clauseAssignments, bodies, 0, 2, qAndR)).post();*/
    }

    private static void configureSearchStrategy() {
        model.getSolver().setSearch(Search.intVarSearch(new Random<>(rng.nextLong()),
                new IntDomainRandom(rng.nextLong()), decisionVariables));
        //solver.setRestartOnSolutions(); // takes much longer, but solutions are more random
    }

    private static void saveProgramsToFiles() throws IOException {
        for (int i = 0; i < NUM_SOLUTIONS && model.getSolver().solve(); i++) {
            //System.out.println("========== " + i + " ==========");
            StringBuilder program = new StringBuilder();
            for (int j = 0; j < MAX_NUM_CLAUSES; j++)
                program.append(clauseToString(j, rng));
            System.out.println(program);

            BufferedWriter writer = new BufferedWriter(new FileWriter(DIRECTORY + i + ".pl"));
            writer.write(program.toString());
            writer.close();
        }
    }

    private static void checkNumPrograms() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("../program_counts.csv"));
        String row;
        while ((row = reader.readLine()) != null) {
            // Read each line of the CSV file into fields
            String[] data = row.split(";");
            List<Integer> arities = Arrays.stream(data[0].substring(1, data[0].length() - 1).split(","))
                    .map(Integer::parseInt).collect(toList());
            List<Integer> predicatesWithArity = Arrays.stream(data[1].substring(1, data[1].length() - 1)
                    .split(",")).map(Integer::parseInt).collect(toList());

            int numPredicates = predicatesWithArity.stream().reduce(0, Integer::sum);
            PREDICATES = new String[numPredicates];
            Arrays.fill(PREDICATES, "p"); // Names don't matter
            ARITIES = new int[numPredicates];
            int k = 0;
            for (int i = 0; i < arities.size(); i++)
                for (int j = 0; j < predicatesWithArity.get(i); j++)
                    ARITIES[k++] = arities.get(i);

            VARIABLES = new String[Integer.parseInt(data[2])];
            Arrays.fill(VARIABLES, "X");
            CONSTANTS = new String[Integer.parseInt(data[3])];
            Arrays.fill(CONSTANTS, "a");
            MAX_NUM_NODES = Integer.parseInt(data[4]);
            MAX_NUM_CLAUSES = Integer.parseInt(data[5]);
            MAX_ARITY = Arrays.stream(ARITIES).max().getAsInt();
            int predictedProgramCount = Integer.parseInt(data[6]);

            // Set up to run the CP solver
            setUp();
            configureSearchStrategy();

            // Count the number of solutions
            int i = 0;
            System.out.println("========================================");
            while (model.getSolver().solve()) {
                i++;
                /*System.out.println("=====Program=====");
                StringBuilder program = new StringBuilder();
                for (int j = 0; j < MAX_NUM_CLAUSES; j++) {
                    program.append(clauseToString(j, rng));
                }
                System.out.println(program);*/
            }
            if (i != predictedProgramCount) {
                System.out.println("Parameters: " + row);
                System.out.println("Number of programs: " + i);
            }
        }
        reader.close();
    }

    public static void main(String[] args) throws IOException {
        setUp();
        //setUpExtraConditions();
        configureSearchStrategy();
        saveProgramsToFiles();

        //checkNumPrograms();
    }
}
