package propagators;

import model.IndependentPair;
import model.Program;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.HashSet;
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
        predicate1 = IndependentPair.toInt(program.predicates, independentPair.getFirst());
        predicate2 = IndependentPair.toInt(program.predicates, independentPair.getSecond());
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
        adjacencyMatrix = independentPair.getCondition().constructAdjacencyMatrix(program.clauseAssignments,
                program.bodies);
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
}
