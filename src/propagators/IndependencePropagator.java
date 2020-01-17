package propagators;

import main.Body;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.HashSet;
import java.util.Set;

/** A custom constraint for the independence of two predicates */
public class IndependencePropagator extends Propagator<IntVar> {

    private IntVar[][] adjacencyMatrix;
    private int predicate1;
    private int predicate2;

    public IndependencePropagator(IntVar[][] adjacencyMatrix, int predicate1, int predicate2) {
        super(ArrayUtils.flatten(adjacencyMatrix));
        this.adjacencyMatrix = adjacencyMatrix;
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        Set<Dependency> dependencies1 = getDependencies(predicate1);
        Set<Dependency> dependencies2 = getDependencies(predicate2);
        for (Dependency dependency1 : dependencies1) {
            for (Dependency dependency2 : dependencies2) {
                if (dependency1.getPredicate() == dependency2.getPredicate()) {
                    if (dependency1.isDetermined() && dependency2.isDetermined())
                        fails();
                    if (dependency1.isDetermined()) {
                        adjacencyMatrix[dependency2.getSource()][dependency2.getTarget()].removeValue(1,
                                this);
                    } else if (dependency2.isDetermined()) {
                        adjacencyMatrix[dependency1.getSource()][dependency1.getTarget()].removeValue(1,
                                this);
                    }
                }
            }
        }
    }

    // TODO: this could be improved (see the pseudocode)
    @Override
    public ESat isEntailed() {
        Set<Dependency> dependencies1 = getDependencies(predicate1);
        Set<Dependency> dependencies2 = getDependencies(predicate2);
        for (Dependency dependency1 : dependencies1) {
            for (Dependency dependency2 : dependencies2) {
                if (dependency1.getPredicate() == dependency2.getPredicate()) {
                    if (dependency1.isDetermined() && dependency2.isDetermined())
                        return ESat.FALSE;
                    return ESat.UNDEFINED;
                }
            }
        }
        return ESat.TRUE;
    }

    /** Return a set of two types of dependencies: those that are guaranteed to be there (determined) and those that
     * could exist if one edge in the dependency graph is instantiated. */
    private Set<Dependency> getDependencies(int initialPredicate) {
        Set<Dependency> dependencies = new HashSet<>();
        dependencies.add(new Dependency(initialPredicate));
        while (true) {
            Set<Dependency> newDependencies = new HashSet<>();
            for (Dependency dependency : dependencies) {
                for (int i = 0; i < adjacencyMatrix.length; i++) {
                    boolean edgeIsDetermined = adjacencyMatrix[i][dependency.getPredicate()].getDomainSize() == 1;
                    boolean edgeExists = adjacencyMatrix[i][dependency.getPredicate()].getValue() == 1;
                    if (edgeIsDetermined && edgeExists && dependency.isDetermined()) {
                        newDependencies.add(new Dependency(i));
                    } else if (edgeIsDetermined && edgeExists) {
                        newDependencies.add(new Dependency(i, dependency.getSource(), dependency.getTarget()));
                    } else if (!edgeIsDetermined && dependency.isDetermined()) {
                        newDependencies.add(new Dependency(i, i, dependency.getPredicate()));
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
