package propagators;

import main.Body;
import main.ForbidCycles;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** A simple propagator to check if the program contains negative cycles (or any cycles) */
public class NegativeCyclePropagator extends Propagator<IntVar> {

    private IntVar[] clauseAssignments;
    private Body[] bodies;
    private ForbidCycles forbidCycles;
    private List<List<SignedPredicate>> adjacencyList;

    public NegativeCyclePropagator(IntVar[] clauseAssignments, Body[] bodies, ForbidCycles forbidCycles) {
        super(constructDecisionVariables(clauseAssignments, bodies));
        this.clauseAssignments = clauseAssignments;
        this.bodies = bodies;
        this.forbidCycles = forbidCycles;
    }

    static IntVar[] constructDecisionVariables(IntVar[] clauseAssignments, Body[] bodies) {
        IntVar[] decisionVariables = clauseAssignments;
        for (Body body : bodies)
            decisionVariables = ArrayUtils.concat(decisionVariables, body.getStructuralDecisionVariables());
        return decisionVariables;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (isEntailed() == ESat.FALSE)
            fails();
    }

    /**
     TRUE: the program cannot contain a negative cycle.
     FALSE: the program must contain a negative cycle.
     UNDEFINED: it may or may not contain a negative cycle.
     */
    @Override
    public ESat isEntailed() {
        // Find all clauses whose structure is completely determined
        List<Integer> determinedClauses = new LinkedList<>();
        for (int i = 0; i < bodies.length; i++) {
            if (clauseAssignments[i].getDomainSize() != 1)
                continue;
            boolean determined = true;
            for (IntVar v : bodies[i].getStructuralDecisionVariables()) {
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

        // Construct the graph
        adjacencyList = new ArrayList<>();
        for (int i = 0; i < predicates.size(); i++)
            adjacencyList.add(new LinkedList<>());
        for (int i : determinedClauses) {
            for (SignedPredicate predicate : bodies[i].getSignedPredicates()) {
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
        boolean forbidAllCycles = false;
        if (this.forbidCycles == ForbidCycles.ALL)
            forbidAllCycles = true;
        for (int i = 0; i < numNodes; i++)
            if (isCyclic(i, forbidAllCycles, visited, recursionStack))
                return ESat.FALSE;

        // If there is no negative cycle and the program is fully determined, then (and only then) can we say that
        // the constraint is satisfied
        if (determinedClauses.size() == bodies.length)
            return ESat.TRUE;
        return ESat.UNDEFINED;
    }

    private boolean isCyclic(int node, boolean encounteredNegativeEdge, boolean[] visited, boolean[] recursionStack) {
        // Mark the current node as visited and part of recursion stack
        if (recursionStack[node] && encounteredNegativeEdge)
            return true;
        if (visited[node])
            return false;
        visited[node] = true;
        recursionStack[node] = true;

        for (SignedPredicate p : adjacencyList.get(node))
            if (isCyclic(p.getIndex(), encounteredNegativeEdge || p.getSign() == Sign.NEG,
                    visited, recursionStack))
                return true;

        recursionStack[node] = false;
        return false;
    }
}
