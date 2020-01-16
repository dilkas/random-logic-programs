package main;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.tools.ArrayUtils;
import propagators.Sign;
import propagators.SignedPredicate;

import java.util.LinkedList;
import java.util.List;

public class Body {

    private int maxNumNodes;
    private IntVar[] treeStructure;
    private Node[] treeValues;
    private IntVar[] arguments; // concatenated arguments of all nodes in treeValues

    public Body(Program program, Model model, IntVar assignment, int maxNumNodes, int clauseIndex) {
        this.maxNumNodes = maxNumNodes;

        // number of nodes in the main tree
        IntVar numNodes = model.intVar("numNodes[" + clauseIndex + "]", 1, maxNumNodes);
        treeStructure = model.intVarArray("treeStructure[" + clauseIndex + "]", maxNumNodes, 0,
                maxNumNodes - 1);
        treeValues = new Node[maxNumNodes];
        for (int i = 0; i < maxNumNodes; i++)
            treeValues[i] = new Node(program, model, clauseIndex, i);

        // Tree structure
        IntVar numTrees = model.intVar("numTrees[" + clauseIndex + "]", 1, maxNumNodes);
        model.tree(treeStructure, numTrees).post();
        model.arithm(treeStructure[0], "=", 0).post(); // the 0th element is always a root

        model.arithm(numTrees, "+", numNodes, "=", maxNumNodes + 1).post();
        model.sort(treeStructure, treeStructure).post();

        for (int i = 0; i < maxNumNodes; i++) {
            Constraint outsideScope = model.arithm(numNodes, "<=", i);
            Constraint mustBeALoop = model.arithm(treeStructure[i], "=", i);
            Constraint fixValue = model.arithm(treeValues[i].getPredicate(), "=", Token.TRUE.ordinal());
            Constraint isRestricted = model.arithm(treeStructure[i], "<", numNodes);
            model.ifThenElse(outsideScope, model.and(mustBeALoop, fixValue), isRestricted);
        }

        // Tree values
        for (int i = 0; i < maxNumNodes; i++) {
            IntVar exactlyZero = model.intVar(0);
            IntVar exactlyOne = model.intVar(1);
            IntVar moreThanOne = model.intVar(2, Math.max(maxNumNodes, 2));

            IntVar[] structureWithoutI = new IntVar[maxNumNodes - 1];
            for (int j = 0; j < i; j++)
                structureWithoutI[j] = treeStructure[j];
            for (int j = i + 1; j < maxNumNodes; j++)
                structureWithoutI[j - 1] = treeStructure[j];

            Constraint isLeaf = model.count(i, structureWithoutI, exactlyZero);
            Constraint isNegation = model.count(i, structureWithoutI, exactlyOne);
            Constraint isInternal = model.count(i, structureWithoutI, moreThanOne);
            Constraint mustBeAConnective = model.member(treeValues[i].getPredicate(),
                    new int[]{Token.AND.ordinal(), Token.OR.ordinal()});

            // Dividing nodes into predicate nodes, negation nodes, and connective nodes
            model.ifOnlyIf(isLeaf, model.arithm(treeValues[i].getPredicate(), ">=", Token.TRUE.ordinal()));
            model.ifOnlyIf(isNegation, model.arithm(treeValues[i].getPredicate(), "=", 0));
            model.ifOnlyIf(isInternal, mustBeAConnective);

            // 'True' (T) is only acceptable for root nodes
            if (i > 0) {
                Constraint notRoot = model.arithm(treeStructure[i], "!=", i);
                Constraint cannotBeTrue = model.arithm(treeValues[i].getPredicate(), "!=", Token.TRUE.ordinal());
                model.ifThen(notRoot, cannotBeTrue);
            }
        }

        // Disable the clause (restrict it to a unique value) if required
        Constraint shouldBeDisabled = model.arithm(assignment, "=", program.predicates.length);
        Constraint oneNode = model.arithm(numNodes, "=", 1);
        Constraint alwaysTrue = model.arithm(treeValues[0].getPredicate(), "=", Token.TRUE.ordinal());
        model.ifThen(shouldBeDisabled, model.and(oneNode, alwaysTrue));

        int numIndices = treeValues.length * program.maxArity;
        arguments = new IntVar[numIndices];
        for (int i = 0; i < treeValues.length; i++)
            System.arraycopy(treeValues[i].getArguments(), 0, arguments, i * program.maxArity,
                    program.maxArity);
    }

    IntVar[] getArguments() {
        return arguments;
    }

    IntVar[] getPredicateVariables() {
        IntVar[] predicates = new IntVar[treeValues.length];
        for (int i = 0; i < treeValues.length; i++)
            predicates[i] = treeValues[i].getPredicate();
        return predicates;
    }

    /** A list of predicates featured in this clause. The sign of each predicate denotes whether the predicate is
     * negated or not (after unfolding all the logical connectives). */
    public List<SignedPredicate> getPredicates() {
        return getPredicates(0);
    }

    private List<SignedPredicate> getPredicates(int index) {
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
            if (firstChild >= maxNumNodes)
                return predicates;

            List<SignedPredicate> descendants = getPredicates(firstChild);
            for (SignedPredicate descendant : descendants)
                descendant.setNegative();
            return descendants;
        }

        // If the node is AND/OR
        for (int i = 0; i < maxNumNodes; i++) {
            if (i != index && treeStructure[i].getValue() == index)
                predicates.addAll(getPredicates(i));
        }
        return predicates;
    }

    private int findFirstChild(int parent) {
        int i = 0;
        for (; i < maxNumNodes; i++)
            if (i != parent && treeStructure[i].getValue() == parent)
                break;
        return i;
    }

    /** The decision variables relevant to negative cycle detection (i.e., everything except arguments) */
    public IntVar[] getStructuralDecisionVariables() {
        IntVar[] variables = treeStructure;
        for (Node treeValue : treeValues)
            variables = ArrayUtils.concat(variables, treeValue.getPredicate());
        return variables;
    }

    public IntVar[] getDecisionVariables() {
        IntVar[] variables = treeStructure;
        for (Node node : treeValues)
            variables = ArrayUtils.concat(variables, node.getDecisionVariables());
        return variables;
    }

    public IntVar[] getTreeStructure() {
        return treeStructure;
    }

    public IntVar[] getTreeValues() {
        IntVar[] predicates = new IntVar[treeValues.length];
        for (int i = 0; i < treeValues.length; i++)
            predicates[i] = treeValues[i].getPredicate();
        return predicates;
    }

    private List<Integer> getDomainValues(IntVar variable, int toSubtract) {
        List<Integer> values = new LinkedList<>();
        DisposableValueIterator it = variable.getValueIterator(true);
        while (it.hasNext()) {
            int value = it.next() - toSubtract;
            if (value >= 0)
                values.add(value);
        }
        it.dispose();
        return values;
    }

    List<Integer> getTreeStructureDomainValues(int index) {
        return getDomainValues(treeStructure[index], 0);
    }

    List<Integer> getTreeValueDomainValues(int index) {
        return getDomainValues(treeValues[index].getPredicate(), Token.values().length);
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
        for (int j = 0; j < maxNumNodes; j++) {
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

    @Override
    public String toString() {
        return treeToString(0) + ".";
    }
}
