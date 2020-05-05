package model;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.ConditionalIndependencePropagator;
import propagators.IndependencePropagator;
import propagators.NegativeCyclePropagator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

public class Program {

    private static final boolean PRINT_DEBUG_INFO = false;

    public Config config;
    public IntVar[] clauseAssignments; // an array of predicates occurring at the heads of clauses
    public Body[] bodies; // the body of each clause

    int maxArity;
    Tuples aritiesTable; // used in defining constraints

    private Model model; // a Choco-specific variable
    private Head[] clauseHeads; // full heads (predicates, variables, constants)
    private IntVar[][] introductions; // for eliminating variable symmetries
    private java.util.Random rng;
    private double[] PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1, 1, 1, 1, 1};

    Program(Config config) {
        this.config = config;
        model = new Model();
        rng = new java.util.Random();
        maxArity = Collections.max(config.arities);
        //model.getSolver().setRestartOnSolutions();

        // Set up constraints
        assert(config.maxNumClauses >= config.predicates.size());
        setUpMainConstraints();
        setUpVariableSymmetryElimination();
        setUpIndependenceConstraints();
        ForbidCycles forbidCycles = config.getForbidCycles();
        if (forbidCycles != ForbidCycles.NONE)
            new Constraint("NoNegativeCycles",
                    new NegativeCyclePropagator(clauseAssignments, bodies, forbidCycles)).post();
        setUpVariableOrdering();

        if (config.requiredFormula != null) {
            BoolVar[] hasRequiredFormula = new BoolVar[bodies.length];
            for (int i = 0; i < bodies.length; i++)
                hasRequiredFormula[i] = bodies[i].hasRequiredFormula();
            BoolVar one = model.boolVar(true);
            model.max(one, hasRequiredFormula).post();
        }
    }

    Program(Config config, double[] probabilities) {
        this(config);
        PROBABILITIES = probabilities;
    }

    // ================================================== CONSTRAINTS ==================================================

    /** Set up all the variables and constraints. */
    private void setUpMainConstraints() {
        int numPredicates = config.predicates.size();
        assert(config.arities.size() == numPredicates);

        aritiesTable = new Tuples();
        for (Token t : Token.values())
            aritiesTable.add(t.ordinal(), 0); // Tokens don't have arities
        for (int i = 0; i < numPredicates; i++)
            aritiesTable.add(Token.values().length + i, config.arities.get(i)); // Predicate arities are predefined
        aritiesTable.add(numPredicates + Token.values().length, 0); // This stands for a disabled clause

        // numbers < PREDICATES.length assign a clause to a predicate, PREDICATES.length is used to discard the clause
        clauseAssignments = model.intVarArray("clauseAssignments", config.maxNumClauses, 0, numPredicates);

        clauseHeads = new Head[clauseAssignments.length];
        for (int i = 0; i < clauseHeads.length; i++)
            clauseHeads[i] = new Head(this, model, clauseAssignments[i], i);

        IntVar numDisabledClauses = model.intVar("numDisabledClauses", 0,
                config.maxNumClauses - numPredicates);
        model.count(numPredicates, clauseAssignments, numDisabledClauses).post();

        // Each possible value (< PREDICATES.length) should be mentioned at least once
        IntVar numDistinctValues = model.intVar("numDistinctValues", numPredicates, numPredicates + 1);
        model.nValues(clauseAssignments, numDistinctValues).post();

        Constraint containsDisabledClause = model.arithm(numDisabledClauses, ">", 0);
        Constraint allValues = model.arithm(numDistinctValues, "=", numPredicates + 1);
        Constraint allButOne = model.arithm(numDistinctValues, "=", numPredicates);
        model.ifThenElse(containsDisabledClause, allValues, allButOne);

        bodies = new Body[config.maxNumClauses];
        for (int i = 0; i < config.maxNumClauses; i++)
            bodies[i] = new Body(this, model, clauseAssignments[i], i);

        // The order of the clauses doesn't matter (and we don't allow duplicates)
        IntVar[][] decisionVariablesPerClause = new IntVar[config.maxNumClauses][];
        for (int i = 0; i < config.maxNumClauses; i++)
            decisionVariablesPerClause[i] = ArrayUtils.concat(clauseHeads[i].getDecisionVariables(),
                    bodies[i].getDecisionVariables());
        model.lexChainLess(decisionVariablesPerClause).post();
    }

    private void setUpVariableSymmetryElimination() {
        if (config.variables.size() <= 1)
            return;
        int numVariables = config.variables.size();

        // Set up termsPerClause to keep track of all variables and constants across each clause
        int numIndices = (config.maxNumNodes + 1) * maxArity;
        /* a concatenation of all terms in each clause (both head and body) (dimension 1 - clauses,
        dimension 2 - positions) */
        IntVar[][] termsPerClause = new IntVar[config.maxNumClauses][numIndices];
        for (int i = 0; i < config.maxNumClauses; i++) {
            System.arraycopy(clauseHeads[i].getArguments(), 0, termsPerClause[i], 0, maxArity);
            System.arraycopy(bodies[i].getArguments(), 0, termsPerClause[i], maxArity,
                    config.maxNumNodes * maxArity);
        }

        // Set up occurrences
        int maxValue = config.constants.size() + numVariables;
        int[] weights = new int[numIndices];
        int[] possibleIndices = new int[numIndices];
        for (int i = 0; i < numIndices; i++) {
            possibleIndices[i] = i;
            weights[i] = i + 1;
        }
        // for each clause and variable, a set of positions with that variable
        SetVar[][] occurrences = model.setVarMatrix("occurrences", config.maxNumClauses, maxValue + 1,
                new int[0], possibleIndices);
        for (int i = 0; i < config.maxNumClauses; i++) {
            model.setsIntsChanneling(occurrences[i], termsPerClause[i]).post();
            for (int j = 0; j <= maxValue; j++)
                for (int k = j + 1; k <= maxValue; k++)
                    model.disjoint(occurrences[i][j], occurrences[i][k]).post();
        }

        // Eliminate variable symmetry
        introductions = new IntVar[config.maxNumClauses][];
        for (int i = 0; i < config.maxNumClauses; i++) {
            introductions[i] = model.intVarArray("introductions[" + i + "]", numVariables, 0, numIndices);
            for (int v = 0; v < numVariables; v++) {
                model.min(occurrences[i][v], weights, 0, introductions[i][v], false).post();
                SetVar[] occurrencesAtI = new SetVar[1];
                occurrencesAtI[0] = occurrences[i][v];
                Constraint noOccurrences = model.nbEmpty(occurrencesAtI, 1);
                Constraint fixMinOccurrence = model.arithm(introductions[i][v], "=", 0);
                model.ifThen(noOccurrences, fixMinOccurrence);
            }
            model.sort(introductions[i], introductions[i]).post();
            model.allDifferentExcept0(introductions[i]).post();
        }

        // A redundant constraint: for each clause, set up a superset of possible introductory values
        int[] potentialIntroductionsStatic = new int[numIndices + 1];
        for (int i = 0; i <= numIndices; i++)
            potentialIntroductionsStatic[i] = i;
        SetVar[] potentialIntroductions = model.setVarArray(config.maxNumClauses, new int[0],
                potentialIntroductionsStatic);
        for (int i = 0; i < introductions.length; i++)
            for (int j = 0; j < introductions[i].length; j++)
                model.member(introductions[i][j], potentialIntroductions[i]).post();

        // A redundant constraint: Nodes with tokens can't hold arguments
        for (int i = 0; i < config.maxNumClauses; i++) {
            IntVar[] treeValues = bodies[i].getPredicates();
            for (int j = 0; j < treeValues.length; j++) {
                int[] forbiddenValuesStatic = new int[maxArity];
                for (int k = 0; k < maxArity; k++)
                    forbiddenValuesStatic[k] = 1 + maxArity * (j + 1) + k;
                SetVar forbiddenValues = model.setVar(forbiddenValuesStatic, forbiddenValuesStatic);
                Constraint disjoint = model.disjoint(potentialIntroductions[i], forbiddenValues);
                Constraint nodeNotLeaf = model.arithm(treeValues[j], "<", Token.values().length);
                model.ifThen(nodeNotLeaf, disjoint);
            }
        }

        // Another redundant constraint: if the introduction is not NULL, then it must be in the occurrences
        // (and vice versa)
        for (int i = 0; i < config.maxNumClauses; i++) {
            for (int j = 0; j < numVariables; j++) {
                Constraint introductionNotNull = model.arithm(introductions[i][j], ">", 0);
                Constraint itMustBeInOccurrences = model.member(model.intOffsetView(introductions[i][j], -1),
                        occurrences[i][j]);
                model.ifOnlyIf(introductionNotNull, itMustBeInOccurrences);
            }
        }
    }

    private void setUpIndependenceConstraints() {
        int numPredicates = config.predicates.size();
        if (numPredicates == 0)
            return;
        IntVar[][] adjacencyMatrix = model.intVarMatrix("adjacencyMatrix", numPredicates, numPredicates,
                0, 1);
        IntVar zero = model.intVar(0);
        for (int i = 0; i < numPredicates; i++) {
            for (int j = 0; j < numPredicates; j++) {
                Constraint noEdge = model.arithm(adjacencyMatrix[i][j], "=", 0);
                Constraint[] clausesAssignedToJHaveNoI = new Constraint[bodies.length];
                for (int k = 0; k < bodies.length; k++) {
                    Constraint notAssignedToJ = model.arithm(clauseAssignments[k], "!=", j);
                    Constraint hasNoI = model.count(i + Token.values().length,
                            bodies[k].getPredicates(), zero);
                    clausesAssignedToJHaveNoI[k] = model.or(notAssignedToJ, hasNoI);
                }
                // A[i][j] = 0 iff there are no clauses such that clauseAssignments[k] = j
                // and i is in clause[k].treeValues
                model.ifOnlyIf(noEdge, model.and(clausesAssignedToJHaveNoI));
            }
        }
        for (int i = 0; i < config.independentPairs.size(); i++) {
            Propagator<IntVar> propagator;
            IndependentPair pair = config.independentPairs.get(i);
            if (pair.isConditional()) {
                propagator = new ConditionalIndependencePropagator(pair, this);
            } else {
                propagator = new IndependencePropagator(adjacencyMatrix, pair, config.predicates);
            }
            new Constraint("independence " + i, propagator).post();
        }
    }

    // ================================================== SOLVING ==================================================

    /** Semi-random variable ordering */
    private void setUpVariableOrdering() {
        int numStrategies = config.maxNumClauses * 4 + 1;
        int numVariables = config.variables.size();
        if (numVariables > 1)
            numStrategies = config.maxNumClauses * 5 + 1;
        IntStrategy[] strategies = new IntStrategy[numStrategies];
        strategies[0] = Search.intVarSearch(new FirstFail(model), new IntDomainRandom(rng.nextLong()),
                clauseAssignments);
        int j = 1;
        for (int i = 0; i < config.maxNumClauses; i++) {
            IntStrategy structuralStrategy = Search.intVarSearch(new FirstFail(model),
                    new IntDomainRandom(rng.nextLong()), bodies[i].getTreeStructure());
            IntStrategy predicateStrategy = Search.intVarSearch(new FirstFail(model),
                    new IntDomainRandom(rng.nextLong()), ArrayUtils.concat(bodies[i].getPredicates()));
            IntStrategy headGapStrategy = Search.intVarSearch(new FirstFail(model),
                    new IntDomainRandom(rng.nextLong()), clauseHeads[i].getArguments());
            strategies[j++] = structuralStrategy;
            strategies[j++] = predicateStrategy;
            strategies[j++] = headGapStrategy;
            if (numVariables > 1) {
                IntStrategy introductionStrategy = Search.intVarSearch(new FirstFail(model),
                        new IntDomainRandom(rng.nextLong()), introductions[i]);
                strategies[j++] = introductionStrategy;
            }
            IntStrategy bodyGapStrategy = Search.intVarSearch(new FirstFail(model),
                    new IntDomainRandom(rng.nextLong()), bodies[i].getArguments());
            strategies[j++] = bodyGapStrategy;
        }
        model.getSolver().setSearch(new StrategiesSequencer(strategies));
    }

    boolean solve() {
        return model.getSolver().solve();
    }

    void saveProgramsToFiles(int numSolutions, String directory) throws IOException {
        Solver solver = model.getSolver();
        if (PRINT_DEBUG_INFO) {
            solver.showDecisions();
            solver.showContradiction();
        }
        for (int i = 0; i < numSolutions && solver.solve(); i++) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(directory + i + ".pl"));
            writer.write(toString());
            writer.close();
        }
    }

    void compileStatistics(int numSolutions, String prefix, String timeout) {
        Solver solver = model.getSolver();
        solver.setGeometricalRestart(10, 2, new FailCounter(model, 1), 100);
        //solver.setRestartOnSolutions();
        if (timeout != null)
            solver.limitTime(timeout);
        for (int i = 0; i < numSolutions; i++) {
            solver.solve();
            System.out.print(prefix + ";");
            solver.printCSVStatistics();
        }
    }

    // ================================================== OUTPUT ==================================================

    /** For fully-determined programs */
    @Override
    public String toString() {
        StringBuilder program = new StringBuilder();
        for (int i = 0; i < config.maxNumClauses; i++)
            program.append(clauseToString(i, rng));
        return program.toString();
    }

    /** The entire clause, i.e., both body and head */
    private String clauseToString(int i, java.util.Random rng) {
        // Is this clause disabled?
        int predicate = clauseAssignments[i].getValue();
        if (predicate == config.predicates.size())
            return "";

        // Add a probability to the statement
        int probability = rng.nextInt(PROBABILITIES.length);
        String probabilityString = "";
        if (PROBABILITIES[probability] < 1)
            probabilityString = PROBABILITIES[probability] + " :: ";

        String body = bodies[i].toString();
        String head = clauseHeads[i].toString();
        if (body.equals("."))
            return probabilityString + head + ".\n";
        return probabilityString + head + " :- " + body + "\n";
    }

    /** For partially-determined programs */
    public void basicToString() {
        System.out.println("====================");
        for (int i = 0; i < clauseAssignments.length; i++) {
            // Print the head
            String head;
            if (clauseAssignments[i].getDomainSize() == 1) {
                int value = clauseAssignments[i].getValue();
                if (value < config.predicates.size()) {
                    head = config.predicates.get(value);
                } else {
                    head = "<none>";
                }
            } else {
                head = "?";
            }
            System.out.println("Head: " + head);
            if (head.equals("<none>"))
                continue;

            // Print the body
            bodies[i].basicToString();
        }
        System.out.println("====================");
    }

}
