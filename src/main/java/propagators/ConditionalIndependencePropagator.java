package propagators;

import model.Body;
import model.IndependentPair;
import model.Program;
import model.Token;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConditionalIndependencePropagator extends Propagator<IntVar> {

    private boolean[][] adjacencyMatrix;
    private Program program;
    private IndependentPair independentPair;
    private int predicate1;
    private int predicate2;

    public ConditionalIndependencePropagator(IndependentPair independentPair, Program program) {
        super(NegativeCyclePropagator.constructDecisionVariables(program.clauseAssignments, program.bodies));
        this.independentPair = independentPair;
        this.program = program;
        predicate1 = IndependentPair.toInt(program.predicates, independentPair.predicate1);
        predicate2 = IndependentPair.toInt(program.predicates, independentPair.predicate2);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (isEntailed() == ESat.FALSE) {
            //System.out.println("Propagation failed");
            fails();
        }
    }

    @Override
    public ESat isEntailed() {
        //program.basicToString();
        adjacencyMatrix = constructAdjacencyMatrix();
        //printAdjacencyMatrix();
        Set<Integer> dependencies1 = getDependencies(predicate1);
        Set<Integer> dependencies2 = getDependencies(predicate2);
        //System.out.println("Dependencies of " + program.predicates[predicate1] + ": " + setToString(dependencies1));
        //System.out.println("Dependencies of " + program.predicates[predicate2] + ": " + setToString(dependencies2));
        dependencies1.retainAll(dependencies2);
        if (!dependencies1.isEmpty()) {
            //System.out.println("Entailment failed");
            return ESat.FALSE;
        }
        if (allDetermined())
            return ESat.TRUE;
        return ESat.UNDEFINED;
    }

    private String setToString(Set<Integer> set) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for (int i : set) {
            if (!first)
                builder.append(", ");
            builder.append(program.predicates[i]);
            first = false;
        }
        builder.append("}");
        return builder.toString();
    }

    private void printAdjacencyMatrix() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            System.out.print(program.predicates[i] + ":");
            for (int j = 0; j < adjacencyMatrix[i].length; j++) {
                int value = 0;
                if (adjacencyMatrix[i][j])
                    value = 1;
                System.out.print(" " + value);
            }
            System.out.println();
        }
    }

    /** Check if all predicates and structural variables are determined */
    private boolean allDetermined() {
        for (int i = 0; i < program.bodies.length; i++) {
            if (program.clauseAssignments[i].getDomainSize() != 1)
                return false;
            if (!program.bodies[i].allDetermined())
                return false;
        }
        return true;
    }

    private Set<Integer> getDependencies(int initialPredicate) {
        Set<Integer> dependencies = new HashSet<>();
        dependencies.add(initialPredicate);
        while (true) {
            Set<Integer> newDependencies = new HashSet<>();
            for (int dependency : dependencies)
                for (int i = 0; i < adjacencyMatrix.length; i++)
                    if (adjacencyMatrix[i][dependency])
                        newDependencies.add(i);
            int previousSize = dependencies.size();
            dependencies.addAll(newDependencies);
            if (dependencies.size() == previousSize)
                break;
        }
        return dependencies;
    }

    private boolean[][] constructAdjacencyMatrix() {
        List<String> predicates = independentPair.condition.predicates;
        int[] conditionedPredicates = new int[predicates.size()]; // shifted by Token.values().length
        for (int i = 0; i < predicates.size(); i++) {
            for (int j = 0; j < program.predicates.length; j++) {
                if (predicates.get(i).equals(program.predicates[j])) {
                    conditionedPredicates[i] = Token.values().length + j;
                    break;
                }
            }
        }

        // A[i][j] = there is an edge from i to j = predicate i is in the body while predicate j is the head
        boolean[][] A = new boolean[program.predicates.length][program.predicates.length];
        for (int clause = 0; clause < program.bodies.length; clause++) {
            // All predicates and the structure of the clause must be determined
            if (!allDetermined(program.clauseAssignments[clause], program.bodies[clause])) {
                //System.out.println("Skipped");
                continue;
            }

            IntVar[] structure = program.bodies[clause].getTreeStructure();
            IntVar[] values = program.bodies[clause].getPredicates();
            boolean[] allMaskedIndices = new boolean[structure.length];
            // For each possible root node of the condition
            for (int root = 0; root < structure.length; root++) {
                if (structure[root].getValue() != independentPair.condition.getOperator().ordinal())
                    continue;
                boolean[] foundPredicates = new boolean[conditionedPredicates.length];
                boolean[] maskedIndices = new boolean[structure.length];
                for (int i = 0; i < maskedIndices.length; i++) {
                    if (structure[i].getValue() != root)
                        continue;
                    for (int p = 0; p < foundPredicates.length; p++) {
                        if (!foundPredicates[p] && values[i].getValue() == conditionedPredicates[p]) {
                            foundPredicates[p] = true;
                            maskedIndices[i] = true;
                            break;
                        }
                    }
                }

                // If we found the entire condition, merge it with other instances of the same condition
                if (allTrue(foundPredicates))
                    for (int i = 0; i < maskedIndices.length; i++)
                        if (maskedIndices[i])
                            allMaskedIndices[i] = true;
            }

            // Print the mask (for testing purposes)
            /*for (int i = 0; i < allMaskedIndices.length; i++) {
                if (i > 0)
                    System.out.print(" ");
                if (allMaskedIndices[i]) {
                    System.out.print("1");
                } else {
                    System.out.print("0");
                }
            }
            System.out.println();*/

            // Add edges to the graph
            int headPredicate = program.clauseAssignments[clause].getValue();
            for (int i = 0; i < values.length; i++) {
                int bodyPredicate = values[i].getValue() - Token.values().length;
                if (bodyPredicate >= 0 && !A[headPredicate][bodyPredicate] && !allMaskedIndices[i])
                    A[headPredicate][bodyPredicate] = true;
            }
        }
        return A;
    }

    private boolean allDetermined(IntVar head, Body body) {
        if (head.getDomainSize() != 1)
            return false;
        return body.allDetermined();
    }

    private boolean allTrue(boolean[] array) {
        for (boolean b : array)
            if (!b)
                return false;
        return true;
    }
}
