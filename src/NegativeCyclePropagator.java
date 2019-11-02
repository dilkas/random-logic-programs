import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** A simple propagator to check if the program contains negative cycles */
class NegativeCyclePropagator extends Propagator<IntVar> {

    private IntVar[] clauseAssignments;
    private Clause[] clauses;
    private boolean forbidAllCycles;
    private List<List<SignedPredicate>> adjacencyList;

    private static IntVar[] constructDecisionVariables(IntVar[] clauseAssignments, Clause[] clauses) {
        IntVar[] decisionVariables = clauseAssignments;
        for (Clause clause : clauses)
            decisionVariables = ArrayUtils.concat(decisionVariables, clause.getDecisionVariables());
        return decisionVariables;
    }

    NegativeCyclePropagator(IntVar[] clauseAssignments, Clause[] clauses, boolean forbidAllCycles) {
        super(constructDecisionVariables(clauseAssignments, clauses));
        this.clauseAssignments = clauseAssignments;
        this.clauses = clauses;
        this.forbidAllCycles = forbidAllCycles;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (isEntailed() == ESat.FALSE)
            fails();
    }

    /* Adapted from http://geeksforgeeks.org/detect-cycle-in-a-graph/ */
    private boolean isCyclic(int node, boolean encounteredNegativeEdge, boolean[] visited, boolean[] recursionStack) {
        // Mark the current node as visited and part of recursion stack
        if (recursionStack[node] && encounteredNegativeEdge)
            return true;
        if (visited[node])
            return false;
        visited[node] = true;
        recursionStack[node] = true;

        for (SignedPredicate p : adjacencyList.get(node)) {
            if (isCyclic(p.getIndex(), encounteredNegativeEdge || p.getSign() == Sign.NEG,
                    visited, recursionStack))
                return true;
        }
        recursionStack[node] = false;
        return false;
    }

    /*
    TRUE: the program cannot contain a negative cycle
    FALSE: the program must contain a negative cycle
    UNDEFINED: it may or may not contain a negative cycle
     */
    @Override
    public ESat isEntailed() {
        // Find all clauses that are completely determined (including their assignments)
        List<Integer> determinedClauses = new LinkedList<>();
        for (int i = 0; i < clauses.length; i++) {
            if (clauseAssignments[i].getDomainSize() != 1)
                continue;
            boolean determined = true;
            for (IntVar v : clauses[i].getDecisionVariables()) {
                if (v.getDomainSize() != 1) {
                    determined = false;
                    break;
                }
            }
            if (determined)
                determinedClauses.add(i);
        }

        if (determinedClauses.isEmpty())
            return ESat.UNDEFINED;

        // Construct a list of unique predicates that we're considering
        List<Integer> predicates = new ArrayList<>();
        for (int i : determinedClauses) {
            int predicate = clauseAssignments[i].getValue();
            if (predicates.isEmpty() || predicates.get(predicates.size() - 1) != predicate) {
                predicates.add(predicate);
            }
        }

        // Initialise the adjacency list
        adjacencyList = new ArrayList<>();
        for (int i = 0; i < predicates.size(); i++)
            adjacencyList.add(new LinkedList<>());
        for (int i : determinedClauses) {
            for (SignedPredicate predicate : clauses[i].getPredicates()) {
                int index1 = predicates.indexOf(clauseAssignments[i].getValue());
                int index2 = predicates.indexOf(predicate.getIndex());
                if (index2 == -1)
                    continue;
                // Renaming predicates to start from zero
                adjacencyList.get(index1).add(new SignedPredicate(index2, predicate.getSign()));
            }
        }

        // Check for cycles
        int numNodes = adjacencyList.size();
        boolean[] visited = new boolean[numNodes];
        boolean[] recursionStack = new boolean[numNodes];
        for (int i = 0; i < numNodes; i++)
            if (isCyclic(i, forbidAllCycles, visited, recursionStack))
                return ESat.FALSE;

        // If there is no negative cycle and the program is fully determined, then (and only then) can we say that
        // the constraint is satisfied
        if (determinedClauses.size() == clauses.length) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
