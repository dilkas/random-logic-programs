package model;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public int maxNumNodes;
    public int maxNumClauses;
    public String forbidCycles;
    public Formula requiredFormula;

    public List<String> predicates;
    public List<Integer> arities;
    public List<String> variables;
    public List<String> constants;
    public List<IndependentPair> independentPairs;

    // Needed for Jackson
    public Config() {
        predicates = new ArrayList<>();
        arities = new ArrayList<>();
        variables = new ArrayList<>();
        constants = new ArrayList<>();
        independentPairs = new ArrayList<>();
    }

    public Config(int maxNumNodes, int maxNumClauses, String forbidCycles, List<String> predicates,
                  List<Integer> arities, List<String> variables, List<String> constants,
                  List<IndependentPair> independentPairs, Formula requiredFormula) {
        this.maxNumNodes = maxNumNodes;
        this.maxNumClauses = maxNumClauses;
        this.forbidCycles = forbidCycles;
        this.predicates = predicates;
        this.arities = arities;
        this.variables = variables;
        this.constants = constants;
        this.independentPairs = independentPairs;
        this.requiredFormula = requiredFormula;
    }

    public ForbidCycles getForbidCycles() {
        if (forbidCycles.equals("NONE"))
            return ForbidCycles.NONE;
        if (forbidCycles.equals("NEGATIVE"))
            return ForbidCycles.NEGATIVE;
        if (forbidCycles.equals("ALL"))
            return ForbidCycles.ALL;
        throw new IllegalArgumentException();
    }
}
