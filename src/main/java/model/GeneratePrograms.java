package model;

import java.io.*;
import java.util.*;

import org.chocosolver.util.tools.ArrayUtils;
import propagators.Condition;

import static java.util.stream.Collectors.toList;

class GeneratePrograms {

    private static final double[] DEFAULT_PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
            1, 1, 1, 1, 1, 1};
    private static final int NUM_SOLUTIONS = 1000;
    private static final String OUTPUT_DIRECTORY = "../programs/";

    private static void checkNumPrograms() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("../program_counts.csv"));
        String row;
        while ((row = reader.readLine()) != null) {
            // Read each line of the CSV file into fields
            String[] data = row.split(";");
            List<Integer> aritiesList = Arrays.stream(data[0].substring(1, data[0].length() - 1).split(", "))
                    .map(Integer::parseInt).collect(toList());
            List<Integer> predicatesWithArity = Arrays.stream(data[1].substring(1, data[1].length() - 1)
                    .split(", ")).map(Integer::parseInt).collect(toList());

            int numPredicates = predicatesWithArity.stream().reduce(0, Integer::sum);
            String[] predicates = new String[numPredicates];
            for (int i = 0; i < numPredicates; i++)
                predicates[i] = "p" + (i + 1);
            int[] arities = new int[numPredicates];
            int k = 0;
            for (int i = 0; i < aritiesList.size(); i++)
                for (int j = 0; j < predicatesWithArity.get(i); j++)
                    arities[k++] = aritiesList.get(i);

            String[] variables = new String[Integer.parseInt(data[2])];
            for (int i = 0; i < variables.length; i++)
                variables[i] = "X" + (i + 1);
            String[] constants = new String[Integer.parseInt(data[3])];
            for (int i = 0; i < constants.length; i++)
                constants[i] = "a" + (i + 1);
            int predictedProgramCount = Integer.parseInt(data[6]);

            Program p = new Program(Integer.parseInt(data[4]), Integer.parseInt(data[5]), ForbidCycles.NONE,
                    new double[]{1}, predicates, arities, variables, constants, new PredicatePair[0]);

            // Count the number of solutions
            int i = 0;
            System.out.print(".");
            //System.out.println("========================================");
            while (p.solve()) {
                i++;
                /*System.out.println("=====Program=====");
                StringBuilder program = new StringBuilder();
                for (int j = 0; j < MAX_NUM_CLAUSES; j++) {
                    program.append(clauseToString(j, rng));
                }
                System.out.println(program);*/
            }
            if (i != predictedProgramCount) {
                System.out.println("Parameters: " + row);
                System.out.println("Number of programs: " + i);
            }
        }
        reader.close();
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

    private static void fillWithNames(String[] array, String prefix) {
        String name = "a";
        for (int i = 0; i < array.length; i++) {
            array[i] = prefix + name;
            name = nextName(name);
        }
    }

    private static List<int[]> generateArities(int[] arities, int numArities, int maxArity) {
        List<int[]> possibilities = new ArrayList<>();
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

    private static List<PredicatePair> generateAllPairsOfPredicates(String[] predicates) {
        List<PredicatePair> pairs = new LinkedList<>();
        for (int i = 0; i < predicates.length; i++)
            for (int j = i + 1; j < predicates.length; j++)
                pairs.add(new PredicatePair(predicates, predicates[i], predicates[j]));
            return pairs;
    }

    private static PredicatePair[] selectRandomSubset(List<PredicatePair> pairs, int M, Random rng) {
        int m = M;
        PredicatePair[] selection = new PredicatePair[m];
        for (int i = 0; i < pairs.size(); i++) {
            if (rng.nextInt(pairs.size() - i) < m) {
                selection[M - m] = pairs.get(i);
                m--;
            }
        }
        return selection;
    }

    private static void generateSmallPrograms() {
        Random rng = new Random();
        for (int numPredicates = 1; numPredicates < 5; numPredicates++) {
            String[] predicates = new String[numPredicates];
            fillWithNames(predicates, "");
            List<PredicatePair> potentialIndependentPairs = generateAllPairsOfPredicates(predicates);

            for (int maxArity = 1; maxArity < 5; maxArity++) {
                List<int[]> potentialArities = generateArities(new int[0], predicates.length, maxArity);

                for (int numVariables = 0; numVariables < 5; numVariables++) {
                    String[] variables = new String[numVariables];
                    fillWithNames(variables, "v");

                    for (int numConstants = 0; numConstants < 5; numConstants++) {
                        if (numConstants == 0 && numVariables == 0)
                            continue;
                        String[] constants = new String[numConstants];
                        fillWithNames(constants, "c");

                        for (int numAdditionalClauses = 0; numAdditionalClauses < 5; numAdditionalClauses++) {
                            int maxNumClauses = predicates.length + numAdditionalClauses;

                            for (int numIndependentPairs = 0;
                                 numIndependentPairs <= numPredicates * (numPredicates - 1) / 2;
                                 numIndependentPairs++) {

                                for (int maxNumNodes = 1; maxNumNodes < 5; maxNumNodes++) {
                                    String prefix = numPredicates + ";" + maxArity + ";" + numVariables + ";" +
                                            numConstants + ";" + numAdditionalClauses + ";" + numIndependentPairs +
                                            ";" + maxNumNodes;

                                    for (int i = 0; i < 10; i++) {
                                        int[] arities = potentialArities.get(rng.nextInt(potentialArities.size()));

                                        for (int j = 0; j < 10; j++) {
                                            PredicatePair[] independentPairs =
                                                    selectRandomSubset(potentialIndependentPairs, numIndependentPairs,
                                                            rng);

                                            Program p = new Program(maxNumNodes, maxNumClauses, ForbidCycles.NEGATIVE,
                                                    DEFAULT_PROBABILITIES, predicates, arities, variables, constants,
                                                    independentPairs);
                                            p.compileStatistics(10, prefix, null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void generateBigPrograms(List<Integer> possibilities) {
        Random rng = new Random();
        for (int numPredicates : possibilities) {
            String[] predicates = new String[numPredicates];
            fillWithNames(predicates, "");
            List<PredicatePair> potentialIndependentPairs = generateAllPairsOfPredicates(predicates);

            for (int maxArity = 1; maxArity < 5; maxArity++) {
                List<int[]> potentialArities = generateArities(new int[0], predicates.length, maxArity);

                for (int numVariables : possibilities) {
                    String[] variables = new String[numVariables];
                    fillWithNames(variables, "v");

                    for (int numConstants : possibilities) {
                        String[] constants = new String[numConstants];
                        fillWithNames(constants, "c");

                        for (int numAdditionalClauses : possibilities) {
                            int maxNumClauses = predicates.length + numAdditionalClauses;

                            for (int numIndependentPairs = 0;
                                 numIndependentPairs <= numPredicates * (numPredicates - 1) / 2;
                                 numIndependentPairs++) {

                                for (int maxNumNodes : possibilities) {
                                    String prefix = numPredicates + ";" + maxArity + ";" + numVariables + ";" +
                                            numConstants + ";" + numAdditionalClauses + ";" + numIndependentPairs +
                                            ";" + maxNumNodes;

                                    for (int i = 0; i < 10; i++) {
                                        int[] arities = potentialArities.get(rng.nextInt(potentialArities.size()));
                                        PredicatePair[] independentPairs = selectRandomSubset(potentialIndependentPairs,
                                                numIndependentPairs, rng);
                                        Program p = new Program(maxNumNodes, maxNumClauses, ForbidCycles.NEGATIVE,
                                                DEFAULT_PROBABILITIES, predicates, arities, variables, constants,
                                                independentPairs);
                                        p.compileStatistics(1, prefix, "60s");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //private runAccordingToConfig() {
        //ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    //}

    public static void main(String[] args) throws IOException {
        //checkNumPrograms();
        //generateSmallPrograms();
        //generateBigPrograms(Arrays.asList(1, 2, 4, 8));

        String[] predicates = new String[]{"p", "q", "r", "s"};
        Program p = new Program(3, 4,
                ForbidCycles.NONE, DEFAULT_PROBABILITIES, predicates, new int[]{1, 1, 1, 1}, new String[]{"X"},
                new String[]{}, new PredicatePair[]{new PredicatePair(predicates, "p", "q",
                new Condition(Token.AND, new String[]{"r", "s"}, predicates))});
        p.saveProgramsToFiles(NUM_SOLUTIONS, OUTPUT_DIRECTORY);
    }
}
