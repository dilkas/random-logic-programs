import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.HashSet;
import java.util.Set;

/** A custom constraint for the independence of two predicates */
class IndependencePropagator extends Propagator<IntVar> {

    private IntVar[][] adjacencyMatrix;
    private int predicate1;
    private int predicate2;

    IndependencePropagator(IntVar[][] adjacencyMatrix, int predicate1, int predicate2) {
        super(ArrayUtils.flatten(adjacencyMatrix));
        this.adjacencyMatrix = adjacencyMatrix;
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (isEntailed() == ESat.FALSE)
            fails();
    }

    @Override
    public ESat isEntailed() {
        Set<Integer> d1 = new HashSet<>();
        d1.add(predicate1);
        Set<Integer> d2 = new HashSet<>();
        d2.add(predicate2);
        ESat answer = independent(d1, d2);
        /*if (answer != ESat.UNDEFINED) {
            System.out.print(matrixToString(adjacencyMatrix));
            System.out.println(answer + "\n");
        }*/
        return answer;
    }

    private String matrixToString(IntVar[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++)
                builder.append(matrix[i][j].getValue()).append(" ");
            builder.append("\n");
        }
        return builder.toString();
    }

    private Dependencies getDependencies(int predicate) {
        Dependencies dependencies = new Dependencies();
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            boolean certain = adjacencyMatrix[i][predicate].getDomainSize() == 1;
            if (!certain) {
                dependencies.setUncertain();
                return dependencies;
            }
            int value = adjacencyMatrix[i][predicate].getValue();
            if (value == 1)
                dependencies.add(i);

        }
        return dependencies;
    }

    private ESat independent(Set<Integer> dependencies1, Set<Integer> dependencies2) {
        Set<Integer> intersection = new HashSet<>(dependencies1);
        intersection.retainAll(dependencies2);
        if (!intersection.isEmpty())
            return ESat.FALSE;

        Set<Integer> newDependencies1 = new HashSet<>(dependencies1);
        Set<Integer> newDependencies2 = new HashSet<>(dependencies2);
        for (int predicate : dependencies1) {
            Dependencies dependencies = getDependencies(predicate);
            if (!dependencies.areCertain())
                return ESat.UNDEFINED;
            newDependencies1.addAll(dependencies.getDependencies());
        }
        for (int predicate : dependencies2) {
            Dependencies dependencies = getDependencies(predicate);
            if (!dependencies.areCertain())
                return ESat.UNDEFINED;
            newDependencies2.addAll(dependencies.getDependencies());
        }

        if (newDependencies1.size() == dependencies1.size() && newDependencies2.size() == dependencies2.size()) {
            return ESat.TRUE;
        }

        return independent(newDependencies1, newDependencies2);
    }
}
