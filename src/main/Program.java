package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.SetDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.GeneralizedMinDomVarSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.SetStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.NegativeCyclePropagator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Program {
    private String directory;
    private int numSolutions;
    private int maxNumNodes;
    private int maxNumClauses;
    private double[] probabilities;

    String[] predicates;
    private int[] ARITIES;
    String[] variables;
    String[] constants;
    int maxArity;

    Tuples arities; // used in defining constraints

    private Model model;
    private IntVar[] clauseAssignments; // an array of predicates occurring at the heads of clauses
    private Head[] clauseHeads; // full heads (predicates, variables, constants)
    private Body[] bodies; // the body of each clause
    private IntVar[] structuralDecisionVariables;
    private IntVar[] predicateDecisionVariables;
    private IntVar[] gapDecisionVariables;
    private IntVar[][] termsPerClause; // a flattened-out view of the term positions in each clause
    private SetVar[][] occurrences; // for each clause and variable, a set of positions with that variable
    private IntVar[][] introductions; // for eliminating variable symmetries
    private java.util.Random rng;

    @SuppressWarnings("unchecked")
    Program(String directory, int numSolutions, int maxNumNodes, int maxNumClauses, ForbidCycles forbidCycles,
            double[] probabilities, String[] predicates, int[] arities, String[] variables, String[] constants) {
        this.directory = directory;
        this.numSolutions = numSolutions;
        this.maxNumNodes = maxNumNodes;
        this.maxNumClauses = maxNumClauses;
        this.probabilities = probabilities;
        this.predicates = predicates;
        ARITIES = arities;
        this.variables = variables;
        this.constants = constants;
        maxArity = Arrays.stream(ARITIES).max().getAsInt();

        setUpConstraints();
        if (forbidCycles != ForbidCycles.NONE)
            new Constraint("NoNegativeCycles",
                    new NegativeCyclePropagator(clauseAssignments, bodies, forbidCycles)).post();

        IntStrategy intStrategy1 = Search.intVarSearch(new Random<>(rng.nextLong()), new IntDomainRandom(rng.nextLong()), structuralDecisionVariables);
        IntStrategy intStrategy2 = Search.intVarSearch(new Random<>(rng.nextLong()), new IntDomainRandom(rng.nextLong()), gapDecisionVariables);
        IntStrategy intStrategy17 = Search.intVarSearch(new Random<>(rng.nextLong()), new IntDomainRandom(rng.nextLong()), predicateDecisionVariables);

        if (variables.length > 1) {
            IntStrategy intStrategy15 = Search.intVarSearch(new Random<>(rng.nextLong()), new IntDomainRandom(rng.nextLong()), ArrayUtils.flatten(introductions));
            SetStrategy setStrategy = Search.setVarSearch(new GeneralizedMinDomVarSelector(), new SetDomainMin(), true, ArrayUtils.flatten(occurrences));
            model.getSolver().setSearch(new StrategiesSequencer(intStrategy1, intStrategy15, setStrategy, intStrategy17, intStrategy2));
        } else {
            model.getSolver().setSearch(new StrategiesSequencer(intStrategy1, intStrategy17, intStrategy2));
        }
        //model.getSolver().setRestartOnSolutions(); // takes much longer, but solutions are more random
    }

    boolean solve() {
        return model.getSolver().solve();
    }

    void saveProgramsToFiles() throws IOException {
        Solver solver = model.getSolver();
        //solver.showDecisions();
        //solver.showContradiction();
        for (int i = 0; i < numSolutions && solver.solve(); i++) {
            //System.out.println("========== " + i + " ==========");
            StringBuilder program = new StringBuilder();
            for (int j = 0; j < maxNumClauses; j++)
                program.append(clauseToString(j, rng));
            //System.out.println(program);

            BufferedWriter writer = new BufferedWriter(new FileWriter(directory + i + ".pl"));
            writer.write(program.toString());
            writer.close();
        }
    }

    String aritiesToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ARITIES.length; i++) {
            if (i > 0)
                sb.append('-');
            sb.append(ARITIES[i]);
        }
        return sb.toString();
    }


    /** The entire clause, i.e., both body and head */
    private String clauseToString(int i, java.util.Random rng) {
        /*for (int j = 0; j < termsPerClause[i].length; j++)
            System.out.print(termsPerClause[i][j].getValue() + " ");
        System.out.println();
        for (int j = 0; j < occurrences[i].length; j++)
            System.out.print(occurrences[i][j].getValue() + " ");
        System.out.println();
        for (int j = 0; j < M[i].length; j++)
            System.out.print(M[i][j].getValue() + " ");
        System.out.println();*/

        /*System.out.print("Occurrences of variables in the body: ");
        SetVar[] bodyOccurrences = bodies[i].getOccurrences();
        for (int j = 0; j < bodyOccurrences.length; j++)
            System.out.print(bodyOccurrences[j].getValue() + " ");
        System.out.println();*/

        // Is this clause disabled?
        int predicate = clauseAssignments[i].getValue();
        if (predicate == predicates.length)
            return "";

        // Add a probability to the statement
        int probability = rng.nextInt(probabilities.length);
        String probabilityString = "";
        if (probabilities[probability] < 1)
            probabilityString = probabilities[probability] + " :: ";

        String body = bodies[i].toString();
        String head = clauseHeads[i].toString();
        if (body.equals("T."))
            return probabilityString + head+ ".\n";
        return probabilityString + head + " :- " + body + "\n";
    }

    /** Set up all the variables and constraints. */
    private void setUpConstraints() {
        assert(predicates.length == ARITIES.length);
        arities = new Tuples();
        for (Token t : Token.values())
            arities.add(t.ordinal(), 0); // Tokens don't have arities
        for (int i = 0; i < ARITIES.length; i++)
            arities.add(Token.values().length + i, ARITIES[i]); // Predicate arities are predefined
        arities.add(predicates.length + Token.values().length, 0); // This stands for a disabled clause

        model = new Model();
        rng = new java.util.Random();

        // numbers < PREDICATES.length assign a clause to a predicate, PREDICATES.length is used to discard the clause
        clauseAssignments = model.intVarArray("clauseAssignments", maxNumClauses, 0, predicates.length);

        clauseHeads = new Head[clauseAssignments.length];
        for (int i = 0; i < clauseHeads.length; i++)
            clauseHeads[i] = new Head(this, model, clauseAssignments[i], i);

        IntVar numDisabledClauses = model.intVar("numDisabledClauses", 0,
                maxNumClauses - predicates.length);
        model.count(predicates.length, clauseAssignments, numDisabledClauses).post();

        // Each possible value (< PREDICATES.length) should be mentioned at least once
        IntVar numDistinctValues = model.intVar("numDistinctValues", predicates.length,
                predicates.length + 1);
        model.nValues(clauseAssignments, numDistinctValues).post();

        Constraint containsDisabledClause = model.arithm(numDisabledClauses, ">", 0);
        Constraint allValues = model.arithm(numDistinctValues, "=", predicates.length + 1);
        Constraint allButOne = model.arithm(numDistinctValues, "=", predicates.length);
        model.ifThenElse(containsDisabledClause, allValues, allButOne);

        bodies = new Body[maxNumClauses];
        for (int i = 0; i < maxNumClauses; i++)
            bodies[i] = new Body(this, model, clauseAssignments[i], maxNumNodes, i);

        // The order of the clauses doesn't matter (but we still allow duplicates)
        IntVar[][] decisionVariablesPerClause = new IntVar[maxNumClauses][];
        for (int i = 0; i < maxNumClauses; i++)
            decisionVariablesPerClause[i] = ArrayUtils.concat(clauseHeads[i].getDecisionVariables(),
                    bodies[i].getDecisionVariables());
        model.lexChainLessEq(decisionVariablesPerClause).post();

        // Adding a graph representation
        IntVar[][] adjacencyMatrix = model.intVarMatrix("adjacencyMatrix", predicates.length, predicates.length,
                0, 1);
        IntVar zero = model.intVar(0);
        for (int i = 0; i < predicates.length; i++) {
            for (int j = 0; j < predicates.length; j++) {
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

        if (variables.length > 1)
            setUpVariableSymmetryElimination();

        // Collect the decision variables (in the right order)
        structuralDecisionVariables = new IntVar[0];
        for (Body body : bodies) // Body structure
            structuralDecisionVariables = ArrayUtils.concat(structuralDecisionVariables, body.getTreeStructure());

        // Head predicates
        predicateDecisionVariables = clauseAssignments;
        for (Body body : bodies) // Body predicates
            predicateDecisionVariables = ArrayUtils.concat(predicateDecisionVariables, body.getPredicateVariables());

        gapDecisionVariables = new IntVar[0];
        for (Head clauseHead : clauseHeads) // Head arguments
            gapDecisionVariables = ArrayUtils.concat(gapDecisionVariables, clauseHead.getArguments());
        for (Body body : bodies) // Body arguments
            gapDecisionVariables = ArrayUtils.concat(gapDecisionVariables, body.getArguments());
    }

    private void setUpVariableSymmetryElimination() {
        // Set up termsPerClause to keep track of all variables and constants across each clause
        int numIndices = (maxNumNodes + 1) * maxArity;
        // a concatenation of all terms in each clause (both head and body) (dimension 1 - clauses, dimension 2 - positions)
        termsPerClause = new IntVar[maxNumClauses][numIndices];
        for (int i = 0; i < maxNumClauses; i++) {
            System.arraycopy(clauseHeads[i].getArguments(), 0, termsPerClause[i], 0, maxArity);
            System.arraycopy(bodies[i].getArguments(), 0, termsPerClause[i], maxArity, maxNumNodes * maxArity);
        }

        // Set up occurrences
        int maxValue = constants.length + variables.length;
        int[] possibleIndices = new int[numIndices];
        for (int i = 0; i < numIndices; i++)
            possibleIndices[i] = i;
        occurrences = model.setVarMatrix("occurrences", maxNumClauses, maxValue + 1, new int[0],
                possibleIndices);
        for (int i = 0; i < maxNumClauses; i++) {
            model.setsIntsChanneling(occurrences[i], termsPerClause[i]).post();
            for (int j = 0; j <= maxValue; j++)
                for (int k = j + 1; k <= maxValue; k++)
                    model.disjoint(occurrences[i][j], occurrences[i][k]).post();
        }

        // Eliminate variable symmetry
        introductions = new IntVar[maxNumClauses][];
        for (int i = 0; i < maxNumClauses; i++) {
            introductions[i] = model.intVarArray("introductions[" + i + "]", variables.length, 0, numIndices);
            for (int v = 0; v < variables.length; v++) {
                model.min(occurrences[i][v], introductions[i][v], false).post();
                SetVar[] occurrencesAtI = new SetVar[1];
                occurrencesAtI[0] = occurrences[i][v];
                Constraint noOccurrences = model.nbEmpty(occurrencesAtI, 1);
                Constraint fixMinOccurrence = model.arithm(introductions[i][v], "=", numIndices);
                model.ifThen(noOccurrences, fixMinOccurrence);
            }
            model.sort(introductions[i], introductions[i]).post();
        }

        // A redundant constraint: for each clause, set up a superset of possible introductory values
        /*int[] potentialIntroductoryValuesStatic = new int[numIndices + 1];
        for (int i = 0; i <= numIndices; i++)
            potentialIntroductoryValuesStatic[i] = i;
        SetVar[] potentialIntroductoryValues = model.setVarArray(maxNumClauses, new int[0], potentialIntroductoryValuesStatic);
        for (int i = 0; i < introductions.length; i++)
            for (int j = 0; j < introductions[i].length; j++)
                model.member(introductions[i][j], potentialIntroductoryValues[i]).post();

        // A redundant constraint: part 2
        for (int i = 0; i < maxNumClauses; i++) {
            IntVar[] treeValues = bodies[i].getTreeValues();
            for (int j = 0; j < treeValues.length; j++) {
                int[] forbiddenValuesStatic = new int[maxArity];
                for (int k = 0; k < maxArity; k++)
                    forbiddenValuesStatic[k] = maxArity * (j + 1) + k;
                SetVar forbiddenValues = model.setVar(forbiddenValuesStatic, forbiddenValuesStatic);
                Constraint disjoint = model.disjoint(potentialIntroductoryValues[i], forbiddenValues);
                Constraint nodeNotLeaf = model.arithm(treeValues[j], "<", Token.values().length);
                model.ifThen(nodeNotLeaf, disjoint);
            }
        }*/
    }
}
