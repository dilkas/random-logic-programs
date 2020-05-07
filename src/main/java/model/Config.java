package model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {

    public static final String FILENAME = "config.yaml";
    public static final String PROGRAM_COUNTS_FILENAME = "data/program_counts.csv";

    public final boolean allowEmptyBodies = true;
    public final boolean defineEachPredicate = true;
    public final boolean printDebugInfo = false;
    public final boolean printProgramsWhileCounting = false;
    public final int numSolutions = 10;
    public final String outputDirectory = "generated/programs/";

    public int maxNumNodes;
    public int maxNumClauses;
    public String forbidCycles;
    public String prefix; // for output files
    public String timeout;
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

    public Config(int maxNumNodes, int maxNumClauses, String forbidCycles, String timeout, List<String> predicates,
                  List<Integer> arities, List<String> variables, List<String> constants,
                  List<IndependentPair> independentPairs, Formula requiredFormula) {
        this.maxNumNodes = maxNumNodes;
        this.maxNumClauses = maxNumClauses;
        this.forbidCycles = forbidCycles;
        this.timeout = timeout;
        this.predicates = predicates;
        this.arities = arities;
        this.variables = variables;
        this.constants = constants;
        this.independentPairs = independentPairs;
        this.requiredFormula = requiredFormula;
    }

    public static Config initialiseFromFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(FILENAME), Config.class);
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
