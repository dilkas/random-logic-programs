package main;

import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;

import java.util.*;

/** An expression of the form a /\ b or a \/ b for some predicates a and b. */
public class Mask {

    private Token connective;
    private List<Integer> predicates;

    public Mask(Token connective, List<Integer> predicates) {
        this.connective = connective;
        this.predicates = predicates;
    }

    /** Return the set of indices where the head of the AND/OR expression could be, according to the information at
     * index `index' (which could be the head or one of the elements inside the AND/OR expression). */
    private Set<PotentialValue> potentialRoots(Body body, int index) {
        IntVar treeValue = body.getTreeValues()[index];
        DisposableValueIterator it = treeValue.getValueIterator(true);
        Set<PotentialValue> possibleMasks = new HashSet<>();
        while ( it.hasNext()) {
            int value = it.next();
            if (value == connective.ordinal()) {
                possibleMasks.add(new PotentialValue(index, treeValue.getDomainSize() == 1));
            } else if (predicates.contains(value)) {
                List<Integer> potentialHeads = body.getTreeStructureDomainValues(index);
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
    private void markInstances(Body body, Possibility[] masked) {
/*        IntVar[] structure = body.getTreeStructure();
        IntVar[] values = body.getTreeValues();
        Token[] tokens = Token.values();

        // Old stuff
        List<Set<PotentialPredicate>> predicatesPerMask = new ArrayList<>();
        for (int i = 0; i < masked.length; i++)
            predicatesPerMask.add(new HashSet<>());
        List<Set<PotentialValue>> masksPerIndex = new ArrayList<>();
        for (int i = 0; i < structure.length; i++) {
            Set<PotentialValue> iMasks = potentialRoots(body, i);
            masksPerIndex.add(iMasks);
            for (PotentialValue mask : iMasks) {
                if (mask.getValue() == i)
                    continue;
                List<Integer> potentialPredicates = body.getTreeValueDomainValues(i);
                predicatesPerMask.get(mask.getValue()).add(new PotentialPredicate(potentialPredicates, predicates));
            }
        }

        // Generate matrices A and B from notes
        // A: position * (predicates + signs): can this position represent this predicate or sign (based on values[])
        Possibility[][] predicateAtIndex = new Possibility[values.length][tokens.length + predicates.size()];
        for (int i = 0; i < values.length; i++) {
            Arrays.fill(predicateAtIndex[i], Possibility.NO);
            DisposableValueIterator it = values[i].getValueIterator(true);
            while (it.hasNext()) {
                int value = it.next();
                if (values[i].getDomainSize() == 1)
                    predicateAtIndex[i][value] = Possibility.YES;
                else
                    predicateAtIndex[i][value] = Possibility.MAYBE;
            }
            it.dispose();
        }

        // B: related[i][j] => position i can/must be the parent of position j
        Possibility[][] related = new Possibility[structure.length][structure.length];
        for (int i = 0; i < structure.length; i++) {
            for (int j = 0; j < structure.length; j++) {
                if (structure[j].getDomainSize() == 1 && values[i].getDomainSize() == 1 && structure[j].contains(i) &&
                        values[i].contains(connective.ordinal())) {
                    related[i][j] = Possibility.YES;
                } else if (structure[j].contains(i) && values[i].contains(connective.ordinal())) {
                    related[i][j] = Possibility.MAYBE;
                } else {
                    related[i][j] = Possibility.NO;
                }
            }
        }

        // For each potential root node of the expression
        for (int i = 0; i < structure.length; i++) {
            // Check that it can be the root
            if (!structure[i].contains(i) || !values[i].contains(connective.ordinal()))
                continue;

            // TODO: do I need this?
            // determinedToBe[a] lists all nodes that are determined to correspond to predicate a
            // in the expression rooted at i
            List<List<Integer>> determinedToBe = new ArrayList<>();
            for (int j = 0; j < predicates.size(); j++)
                determinedToBe.add(new LinkedList<>());
        }
    }

    // TODO: describe
    public Possibility[] applyMask(Body[] clauses, IntVar[] clauseAssignments, int predicate) {
        Possibility[] masks = new Possibility[GeneratePrograms.PREDICATES.length];
        Arrays.fill(masks, Possibility.NO);
        for (int i = 0; i < clauses.length; i++)
            if (clauseAssignments[i].getValue() == predicate)
                markInstances(clauses[i], masks);
        return masks;
        */
    }
}
