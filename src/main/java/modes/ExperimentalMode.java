package modes;

import model.Config;
import model.ForbidCycles;
import model.IndependentPair;
import model.Program;
import org.chocosolver.util.tools.ArrayUtils;
import java.util.*;

public class ExperimentalMode {

    private static final List<Integer> VALUES = Arrays.asList(1, 2, 4, 8);
    private static final ForbidCycles forbidCycles = ForbidCycles.NEGATIVE;
    private static final int MIN_MAX_ARITY = 1;
    private static final int MAX_MAX_ARITY = 4;
    private static final int NUM_REPEATS = 10;
    private static final int SOLUTIONS_PER_RUN = 1;
    private static final String TIMEOUT = "60s";

    private static final String PREDICATE_PREFIX = "p";
    private static final String VARIABLE_PREFIX = "v";
    private static final String CONSTANT_PREFIX = "c";

    public static void run() {
        Random rng = new Random();
        for (int numPredicates : VALUES) {
            String[] predicates = new String[numPredicates];
            fillWithNames(predicates, PREDICATE_PREFIX);
            List<IndependentPair> potentialIndependentPairs = generateAllPairsOfPredicates(predicates);

            for (int maxArity = MIN_MAX_ARITY; maxArity < MAX_MAX_ARITY; maxArity++) {
                List<Integer[]> potentialArities = generateArities(new Integer[0], predicates.length, maxArity);

                for (int numVariables : VALUES) {
                    String[] variables = new String[numVariables];
                    fillWithNames(variables, VARIABLE_PREFIX);

                    for (int numConstants : VALUES) {
                        String[] constants = new String[numConstants];
                        fillWithNames(constants, CONSTANT_PREFIX);

                        for (int numAdditionalClauses : VALUES) {
                            int maxNumClauses = predicates.length + numAdditionalClauses;

                            for (int numIndependentPairs = 0;
                                 numIndependentPairs <= numPredicates * (numPredicates - 1) / 2;
                                 numIndependentPairs++) {

                                for (int maxNumNodes : VALUES) {
                                    String prefix = String.join(";", Integer.toString(numPredicates),
                                            Integer.toString(maxArity), Integer.toString(numVariables),
                                            Integer.toString(numConstants), Integer.toString(numAdditionalClauses),
                                            Integer.toString(numIndependentPairs), Integer.toString(maxNumNodes));

                                    for (int i = 0; i < NUM_REPEATS; i++) {
                                        Integer[] arities = potentialArities.get(rng.nextInt(potentialArities.size()));
                                        IndependentPair[] independentPairs =
                                                selectRandomSubset(potentialIndependentPairs, numIndependentPairs, rng);
                                        Config config = new Config(maxNumNodes, maxNumClauses, forbidCycles.toString(),
                                                Arrays.asList(predicates), Arrays.asList(arities),
                                                Arrays.asList(variables), Arrays.asList(constants),
                                                Arrays.asList(independentPairs), null);
                                        Program p = new Program(config);
                                        p.compileStatistics(SOLUTIONS_PER_RUN, prefix, TIMEOUT);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void fillWithNames(String[] array, String prefix) {
        String name = "a";
        for (int i = 0; i < array.length; i++) {
            array[i] = prefix + name;
            name = nextName(name);
        }
    }

    private static String nextName(String name) {
        char lastChar = name.charAt(name.length() - 1);
        if (lastChar < 'z')
            return name.substring(0, name.length() - 1) + (char)(lastChar + 1);

        int firstZ;
        for (firstZ = name.length() - 1; firstZ >= 0 && name.charAt(firstZ) == 'z'; firstZ--)
            ;
        firstZ++;

        int countZ = name.length() - firstZ;
        String as = new String(new char[countZ]).replace("\0", "a");
        if (firstZ - 1 >= 0)
            return name.substring(0, firstZ - 1) + (char)(name.charAt(firstZ - 1) + 1) + as;
        return "a" + as;
    }

    private static IndependentPair[] selectRandomSubset(List<IndependentPair> pairs, int M, Random rng) {
        int m = M;
        IndependentPair[] selection = new IndependentPair[m];
        for (int i = 0; i < pairs.size(); i++) {
            if (rng.nextInt(pairs.size() - i) < m) {
                selection[M - m] = pairs.get(i);
                m--;
            }
        }
        return selection;
    }

    private static List<IndependentPair> generateAllPairsOfPredicates(String[] predicates) {
        List<IndependentPair> pairs = new LinkedList<>();
        for (int i = 0; i < predicates.length; i++)
            for (int j = i + 1; j < predicates.length; j++)
                pairs.add(new IndependentPair(predicates[i], predicates[j]));
        return pairs;
    }

    private static List<Integer[]> generateArities(Integer[] arities, int numArities, int maxArity) {
        List<Integer[]> possibilities = new ArrayList<>();
        if (arities.length >= numArities) {
            possibilities.add(arities);
            return possibilities;
        }
        int firstArity = maxArity;
        int lastArity = maxArity;
        if (arities.length > 0) {
            firstArity = arities[arities.length - 1];
            lastArity = 0;
        }

        for (int i = firstArity; i >= lastArity; i--)
            possibilities.addAll(generateArities(ArrayUtils.concat(arities, i), numArities, maxArity));
        return possibilities;
    }
}
