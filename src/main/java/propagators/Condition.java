package propagators;

import model.Body;
import model.Token;
import org.chocosolver.solver.variables.IntVar;

/** Used to represent the condition part of a conditional independence expression */
public class Condition {

    private Token operator;
    private int[] conditionedPredicates; // shifted by Token.values().length
    private int numPredicates;

    public Condition(Token operator, String[] conditionedPredicates, String[] predicates) {
        this.operator = operator;
        this.numPredicates = predicates.length;

        this.conditionedPredicates = new int[conditionedPredicates.length];
        for (int i = 0; i < conditionedPredicates.length; i++) {
            for (int j = 0; j < predicates.length; j++) {
                if (conditionedPredicates[i].equals(predicates[j])) {
                    this.conditionedPredicates[i] = Token.values().length + j;
                    break;
                }
            }
        }
    }

    boolean[][] constructAdjacencyMatrix(IntVar[] clauseAssignments, Body[] bodies) {
        // A[i][j] = there is an edge from i to j = predicate i is in the body while predicate j is the head
        boolean[][] A = new boolean[numPredicates][numPredicates];
        for (int clause = 0; clause < bodies.length; clause++) {
            // All predicates and the structure of the clause must be determined
            if (!allDetermined(clauseAssignments[clause], bodies[clause])) {
                //System.out.println("Skipped");
                continue;
            }

            IntVar[] structure = bodies[clause].getTreeStructure();
            IntVar[] values = bodies[clause].getPredicates();
            boolean[] allMaskedIndices = new boolean[structure.length];
            // For each possible root node of the condition
            for (int root = 0; root < structure.length; root++) {
                if (structure[root].getValue() != operator.ordinal())
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
            int headPredicate = clauseAssignments[clause].getValue();
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
