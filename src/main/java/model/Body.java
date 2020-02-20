package model;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.Sign;
import propagators.SignedPredicate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Body {

    private Program program;
    private IntVar[] treeStructure;
    private Node[] treeValues;
    // The decision variables relevant to negative cycle detection (i.e., everything except arguments)
    private IntVar[] structuralDecisionVariables;

    Body(Program program, Model model, IntVar assignment, int clauseIndex) {
        this.program = program;

        // Auxiliary variables
        IntVar numNodes = model.intVar("numNodes[" + clauseIndex + "]", 1, program.config.maxNumNodes);
        IntVar numTrees = model.intVar("numTrees[" + clauseIndex + "]", 1, program.config.maxNumNodes);
        model.arithm(numTrees, "+", numNodes, "=", program.config.maxNumNodes + 1).post();

        // Tree structure
        treeStructure = model.intVarArray("treeStructure[" + clauseIndex + "]", program.config.maxNumNodes, 0,
                program.config.maxNumNodes - 1);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post(); // the 0th element is always a root
        model.sort(treeStructure, treeStructure).post();

        // Tree values
        treeValues = new Node[program.config.maxNumNodes];
        for (int i = 0; i < program.config.maxNumNodes; i++)
            treeValues[i] = new Node(program, model, clauseIndex, i);

        // Per-node constraints
        for (int i = 0; i < program.config.maxNumNodes; i++) {
            // The first numNodes elements define our tree. Fix the rest to some predefined values.
            if (i > 0) {
                Constraint outsideScope = model.arithm(numNodes, "<=", i);
                Constraint mustBeALoop = model.arithm(treeStructure[i], "=", i);
                Constraint fixValue = model.arithm(treeValues[i].getPredicate(), "=", Token.TRUE.ordinal());
                Constraint isRestricted = model.arithm(treeStructure[i], "<", i);
                model.ifThenElse(outsideScope, model.and(mustBeALoop, fixValue), isRestricted);
            }

            IntVar exactlyZero = model.intVar(0);
            IntVar exactlyOne = model.intVar(1);
            IntVar moreThanOne = model.intVar(2, Math.max(program.config.maxNumNodes, 2));

            IntVar[] potentialChildren = Arrays.copyOfRange(treeStructure, i + 1, treeStructure.length);

            Constraint isLeaf = model.count(i, potentialChildren, exactlyZero);
            Constraint isNegation = model.count(i, potentialChildren, exactlyOne);
            Constraint isInternal = model.count(i, potentialChildren, moreThanOne);
            Constraint mustBeAConnective = model.member(treeValues[i].getPredicate(),
                    new int[]{Token.AND.ordinal(), Token.OR.ordinal()});

            // Dividing nodes into predicate nodes, negation nodes, and connective nodes
            model.ifOnlyIf(isLeaf, model.arithm(treeValues[i].getPredicate(), ">=", Token.TRUE.ordinal()));
            model.ifOnlyIf(isNegation, model.arithm(treeValues[i].getPredicate(), "=", 0));
            model.ifOnlyIf(isInternal, mustBeAConnective);

            // 'True' is only acceptable for root nodes
            if (i > 0) {
                Constraint notRoot = model.arithm(treeStructure[i], "!=", i);
                Constraint cannotBeTrue = model.arithm(treeValues[i].getPredicate(), "!=", Token.TRUE.ordinal());
                model.ifThen(notRoot, cannotBeTrue);
            }
        }

        // Disable the clause (restrict it to a unique value) if required
        Constraint shouldBeDisabled = model.arithm(assignment, "=", program.config.predicates.size());
        Constraint oneNode = model.arithm(numNodes, "=", 1);
        Constraint alwaysTrue = model.arithm(treeValues[0].getPredicate(), "=", Token.TRUE.ordinal());
        model.ifThen(shouldBeDisabled, model.and(oneNode, alwaysTrue));

        structuralDecisionVariables = treeStructure;
        for (Node treeValue : treeValues)
            structuralDecisionVariables = ArrayUtils.concat(structuralDecisionVariables, treeValue.getPredicate());
    }

    // ========================================= GETTERS OF DECISION VARIABLES =======================================

    IntVar[] getArguments() {
        int numIndices = treeValues.length * program.maxArity;
        IntVar[] arguments = new IntVar[numIndices];
        for (int i = 0; i < treeValues.length; i++)
            System.arraycopy(treeValues[i].getArguments(), 0, arguments, i * program.maxArity,
                    program.maxArity);
        return arguments;
    }

    public IntVar[] getPredicates() {
        IntVar[] predicates = new IntVar[treeValues.length];
        for (int i = 0; i < treeValues.length; i++)
            predicates[i] = treeValues[i].getPredicate();
        return predicates;
    }

    public IntVar[] getStructuralDecisionVariables() {
        return structuralDecisionVariables;
    }

    IntVar[] getDecisionVariables() {
        IntVar[] variables = treeStructure;
        for (Node node : treeValues)
            variables = ArrayUtils.concat(variables, node.getDecisionVariables());
        return variables;
    }

    public IntVar[] getTreeStructure() {
        return treeStructure;
    }

    // ======================================== OTHER GETTERS ========================================

    public boolean allDetermined() {
        for (IntVar v : treeStructure)
            if (v.getDomainSize() != 1)
                return false;
        for (Node node : treeValues)
            if (node.getPredicate().getDomainSize() != 1)
                return false;
        return true;
    }

    /** A list of predicates featured in this clause. The sign of each predicate denotes whether the predicate is
     * negated or not (after unfolding all the logical connectives). */
    public List<SignedPredicate> getSignedPredicates() {
        return getSignedPredicates(0);
    }

    private List<SignedPredicate> getSignedPredicates(int index) {
        int valueIndex = treeValues[index].getPredicate().getValue();
        List<SignedPredicate> predicates = new LinkedList<>();

        // If the node is T (true)
        if (valueIndex == Token.TRUE.ordinal())
            return predicates;

        // If the node is a predicate
        if (valueIndex >= Token.values().length) {
            predicates.add(new SignedPredicate(valueIndex - Token.values().length, Sign.POS));
            return predicates;
        }

        // If the node is a NOT
        if (valueIndex == 0) {
            int firstChild = findFirstChild(index);

            // This only happens if the tree constraint is unsatisfied but negative cycle constraint is propagated first
            if (firstChild >= program.config.maxNumNodes)
                return predicates;

            List<SignedPredicate> descendants = getSignedPredicates(firstChild);
            for (SignedPredicate descendant : descendants)
                descendant.setNegative();
            return descendants;
        }

        // If the node is AND/OR
        for (int i = 0; i < program.config.maxNumNodes; i++) {
            if (i != index && treeStructure[i].getValue() == index)
                predicates.addAll(getSignedPredicates(i));
        }
        return predicates;
    }

    /** Return the first index which is a child of the given parent */
    private int findFirstChild(int parent) {
        int i = 0;
        for (; i < program.config.maxNumNodes; i++)
            if (i != parent && treeStructure[i].getValue() == parent)
                break;
        return i;
    }

    // ================================================== OUTPUT ==================================================

    /** For fully-determined bodies */
    @Override
    public String toString() {
        return treeToString(0) + ".";
    }

    private String treeToString(int i) {
        int value = treeValues[i].getPredicate().getValue();
        if (value >= Token.TRUE.ordinal())
            return treeValues[i].toString();
        Token token = Token.values()[value];
        if (token == Token.NOT)
            return token + "(" + treeToString(findFirstChild(i)) + ")";

        boolean first = true;
        StringBuilder output = new StringBuilder();
        for (int j = 0; j < program.config.maxNumNodes; j++) {
            if (j != i && treeStructure[j].getValue() == i) {
                if (first) {
                    first = false;
                } else {
                    output.append(token).append(" ");
                }
                output.append("(").append(treeToString(j)).append(")");
            }
        }
        return output.toString();
    }

    /** For partially-determined bodies */
    void basicToString() {
        System.out.println("Structure: " + structureToString());
        System.out.println("Values:    " + valuesToString());
    }

    private String structureToString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < treeStructure.length; i++) {
            if (i > 0)
                builder.append(" ");
            if (treeStructure[i].getDomainSize() == 1) {
                builder.append(treeStructure[i].getValue());
            } else {
                builder.append("?");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String valuesToString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < treeValues.length; i++) {
            if (i > 0)
                builder.append(" ");
            IntVar predicate = treeValues[i].getPredicate();
            if (predicate.getDomainSize() == 1) {
                int value = predicate.getValue();
                if (value < Token.TRUE.ordinal()) {
                    builder.append(Token.values()[value]);
                } else if (value == Token.TRUE.ordinal()) {
                    builder.append("T");
                } else {
                    builder.append(program.config.predicates.get(value - Token.values().length));
                }
            } else {
                builder.append("?");
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
