import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;

import java.util.*;

/** An expression of the form a /\ b or a \/ b for some predicates a and b. */
public class Mask {

    private Token connective;
    private List<Integer> predicates;

    Mask(Token connective, List<Integer> predicates) {
        this.connective = connective;
        this.predicates = predicates;
    }

    /** Return the set of indices where the head of the AND/OR expression could be, according to the information at
     * index `index' (which could be the head or one of the elements inside the AND/OR expression). */
    private Set<PotentialValue> potentialRoots(Clause clause, int index) {
        IntVar treeValue = clause.getTreeValues()[index];
        DisposableValueIterator it = treeValue.getValueIterator(true);
        Set<PotentialValue> possibleMasks = new HashSet<>();
        while ( it.hasNext()) {
            int value = it.next();
            if (value == connective.ordinal()) {
                possibleMasks.add(new PotentialValue(index, treeValue.getDomainSize() == 1));
            } else if (predicates.contains(value)) {
                List<Integer> potentialHeads = clause.getTreeStructureDomainValues(index);
                if (potentialHeads.size() == 1) {
                    possibleMasks.add(new PotentialValue(potentialHeads.get(0), true));
                } else {
                    for (int h : potentialHeads)
                        possibleMasks.add(new PotentialValue(h, false));
                }
            }
        }
        it.dispose();
        return possibleMasks;
    }

    /** Return a list of size GeneratePrograms.PREDICATES.length of boolean values. True means that the clause's
     * dependence on that variable has been 'masked', i.e., is no longer relevant. */
    private void markInstances(Clause clause, MaskValue[] masked) {
        IntVar[] structure = clause.getTreeStructure();
        IntVar[] values = clause.getTreeValues();

        List<Set<PotentialPredicate>> predicatesPerMask = new ArrayList<>();
        for (int i = 0; i < masked.length; i++)
            predicatesPerMask.add(new HashSet<>());
        List<Set<PotentialValue>> masksPerIndex = new ArrayList<>();
        for (int i = 0; i < structure.length; i++) {
            Set<PotentialValue> iMasks = potentialRoots(clause, i);
            masksPerIndex.add(iMasks);
            for (PotentialValue mask : iMasks) {
                if (mask.getValue() == i)
                    continue;
                List<Integer> potentialPredicates = clause.getTreeValueDomainValues(i);
                predicatesPerMask.get(mask.getValue()).add(new PotentialPredicate(potentialPredicates, predicates));
            }
        }

        // TODO: finish this
        // For each potential root node of the expression
        for (int i = 0; i < structure.length; i++) {
            // Check that it can be the root
            if (!structure[i].contains(i) || !values[i].contains(connective.ordinal()))
                continue;

            boolean[] foundAtLeastOne = new boolean[predicates.size()];
            // determinedToBe[a] lists all nodes that are determined to correspond to predicate a
            // in the expression rooted at i
            List<List<Integer>> determinedToBe = new ArrayList<>();
            for (int i = 0; i < predicates.size(); i++)
                determinedToBe.add(new LinkedList<>());

            // For every node that can be part of the expression rooted at i
            for (PotentialPredicate j : predicatesPerMask.get(i)) {
                if (j.)
            }
        }
    }

    // TODO: describe
    public MaskValue[] applyMask(Clause[] clauses, IntVar[] clauseAssignments, int predicate) {
        MaskValue[] masks = new MaskValue[GeneratePrograms.PREDICATES.length];
        Arrays.fill(masks, MaskValue.UNMASKED);
        for (int i = 0; i < clauses.length; i++)
            if (clauseAssignments[i].getValue() == predicate)
                markInstances(clauses[i], masks);
        return masks;
    }
}
