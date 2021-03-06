package propagators;

import model.IndependentPair;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.dependencies.Dependency;
import propagators.dependencies.Status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A custom constraint for the independence of two predicates */
public class IndependencePropagator extends Propagator<IntVar> {

    private IntVar[][] adjacencyMatrix;
    private int predicate1;
    private int predicate2;

    public IndependencePropagator(IntVar[][] adjacencyMatrix, IndependentPair independentPair,
                                  List<String> predicates) {
        super(ArrayUtils.flatten(adjacencyMatrix));
        this.adjacencyMatrix = adjacencyMatrix;
        predicate1 = IndependentPair.toInt(predicates, independentPair.predicate1);
        predicate2 = IndependentPair.toInt(predicates, independentPair.predicate2);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        Set<Dependency> dependencies1 = getDependencies(predicate1, false);
        Set<Dependency> dependencies2 = getDependencies(predicate2, false);
        for (Dependency dependency1 : dependencies1) {
            for (Dependency dependency2 : dependencies2) {
                if (dependency1.getPredicate() == dependency2.getPredicate()) {
                    if (dependency1.getStatus() == Status.DETERMINED && dependency2.getStatus() == Status.DETERMINED)
                        fails();
                    if (dependency1.getStatus() == Status.DETERMINED) {
                        adjacencyMatrix[dependency2.getSource()][dependency2.getTarget()].removeValue(1, this);
                    } else if (dependency2.getStatus() == Status.DETERMINED) {
                        adjacencyMatrix[dependency1.getSource()][dependency1.getTarget()].removeValue(1, this);
                    }
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {
        Set<Dependency> dependencies1 = getDependencies(predicate1, true);
        Set<Dependency> dependencies2 = getDependencies(predicate2, true);
        for (Dependency dependency1 : dependencies1) {
            for (Dependency dependency2 : dependencies2) {
                if (dependency1.getPredicate() == dependency2.getPredicate()) {
                    if (dependency1.getStatus() == Status.DETERMINED && dependency2.getStatus() == Status.DETERMINED)
                        return ESat.FALSE;
                    return ESat.UNDEFINED;
                }
            }
        }
        return ESat.TRUE;
    }

    private Set<Dependency> getDependencies(int initialPredicate, boolean allDependencies) {
        Set<Dependency> dependencies = new HashSet<>();
        dependencies.add(new Dependency(initialPredicate, true));
        while (true) {
            Set<Dependency> newDependencies = new HashSet<>();
            for (Dependency dependency : dependencies) {
                for (int i = 0; i < adjacencyMatrix.length; i++) {
                    boolean edgeIsDetermined = adjacencyMatrix[i][dependency.getPredicate()].getDomainSize() == 1;
                    boolean edgeExists = edgeIsDetermined &&
                            adjacencyMatrix[i][dependency.getPredicate()].getValue() == 1;
                    if (edgeExists && dependency.getStatus() == Status.DETERMINED) {
                        newDependencies.add(new Dependency(i, true));
                    } else if (edgeExists && dependency.getStatus() == Status.ALMOST_DETERMINED) {
                        newDependencies.add(new Dependency(i, dependency.getSource(), dependency.getTarget()));
                    } else if (!edgeIsDetermined && dependency.getStatus() == Status.DETERMINED) {
                        newDependencies.add(new Dependency(i, i, dependency.getPredicate()));
                    } else if (allDependencies && !edgeIsDetermined) {
                        newDependencies.add(new Dependency(i, false));
                    }
                }
            }
            int previousSize = dependencies.size();
            dependencies.addAll(newDependencies);
            if (dependencies.size() == previousSize)
                break;
        }
        return dependencies;
    }
}
