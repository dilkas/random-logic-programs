package model;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chocosolver.util.tools.ArrayUtils;

import static java.util.stream.Collectors.toList;

class GeneratePrograms {


    private static final int NUM_SOLUTIONS = 1000;
    private static final String OUTPUT_DIRECTORY = "programs/";
    private static final String CONFIG_FILENAME = "config.yaml";
    private static final String PROGRAM_COUNTS_FILENAME = "../program_counts.csv";

    private static void checkNumPrograms() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(PROGRAM_COUNTS_FILENAME));
        String row;
        while ((row = reader.readLine()) != null) {
            // Read each line of the CSV file into fields
            String[] data = row.split(";");
            List<Integer> aritiesList = Arrays.stream(data[0].substring(1, data[0].length() - 1).split(", "))
                    .map(Integer::parseInt).collect(toList());
            List<Integer> predicatesWithArity = Arrays.stream(data[1].substring(1, data[1].length() - 1)
                    .split(", ")).map(Integer::parseInt).collect(toList());

            int numPredicates = predicatesWithArity.stream().reduce(0, Integer::sum);
            List<String> predicates = new ArrayList<>();
            for (int i = 0; i < numPredicates; i++)
                predicates.add("p" + (i + 1));

            List<Integer> arities = new ArrayList<>();
            for (int i = 0; i < aritiesList.size(); i++)
                for (int j = 0; j < predicatesWithArity.get(i); j++)
                    arities.add(aritiesList.get(i));

            int numVariables = Integer.parseInt(data[2]);
            List<String> variables = new ArrayList<>();
            for (int i = 0; i < numVariables; i++)
                variables.add("X" + (i + 1));

            int numConstants = Integer.parseInt(data[3]);
            List<String> constants = new ArrayList<>();
            for (int i = 0; i < numConstants; i++)
                constants.add("a" + (i + 1));
            int predictedProgramCount = Integer.parseInt(data[6]);

            Config config = new Config(Integer.parseInt(data[4]), Integer.parseInt(data[5]), "NONE",
                    predicates, arities, variables, constants, new LinkedList<>(), null);
            Program p = new Program(config, new double[]{1});

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

    private static List<IndependentPair> generateAllPairsOfPredicates(String[] predicates) {
        List<IndependentPair> pairs = new LinkedList<>();
        for (int i = 0; i < predicates.length; i++)
            for (int j = i + 1; j < predicates.length; j++)
                pairs.add(new IndependentPair(predicates[i], predicates[j]));
            return pairs;
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

    private static void generateSmallPrograms() {
        Random rng = new Random();
        for (int numPredicates = 1; numPredicates < 5; numPredicates++) {
            String[] predicates = new String[numPredicates];
            fillWithNames(predicates, "");
            List<IndependentPair> potentialIndependentPairs = generateAllPairsOfPredicates(predicates);

            for (int maxArity = 1; maxArity < 5; maxArity++) {
                List<Integer[]> potentialArities = generateArities(new Integer[0], predicates.length, maxArity);

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
                                        Integer[] arities = potentialArities.get(rng.nextInt(potentialArities.size()));

                                        for (int j = 0; j < 10; j++) {
                                            IndependentPair[] independentPairs =
                                                    selectRandomSubset(potentialIndependentPairs, numIndependentPairs,
                                                            rng);

                                            Config config = new Config(maxNumNodes, maxNumClauses,
                                                    "NEGATIVE", Arrays.asList(predicates),
                                                    Arrays.asList(arities), Arrays.asList(variables),
                                                    Arrays.asList(constants), Arrays.asList(independentPairs),
                                                    null);
                                            Program p = new Program(config);
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
            List<IndependentPair> potentialIndependentPairs = generateAllPairsOfPredicates(predicates);

            for (int maxArity = 1; maxArity < 5; maxArity++) {
                List<Integer[]> potentialArities = generateArities(new Integer[0], predicates.length, maxArity);

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
                                        Integer[] arities = potentialArities.get(rng.nextInt(potentialArities.size()));
                                        IndependentPair[] independentPairs =
                                                selectRandomSubset(potentialIndependentPairs, numIndependentPairs, rng);
                                        Config config = new Config(maxNumNodes, maxNumClauses, "NEGATIVE",
                                                Arrays.asList(predicates), Arrays.asList(arities),
                                                Arrays.asList(variables), Arrays.asList(constants),
                                                Arrays.asList(independentPairs), null);
                                        Program p = new Program(config);
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

    private static void runAccordingToConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Config config = mapper.readValue(new File(CONFIG_FILENAME), Config.class);
        Program p = new Program(config);
        p.saveProgramsToFiles(NUM_SOLUTIONS, OUTPUT_DIRECTORY);
    }

    /** Four types of experiments */
    public static void main(String[] args) throws IOException {
        //checkNumPrograms();
        //generateSmallPrograms();
        //generateBigPrograms(Arrays.asList(1, 2, 4, 8));
        runAccordingToConfig();
    }
}
