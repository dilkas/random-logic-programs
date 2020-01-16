package main;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.chocosolver.util.tools.ArrayUtils;

import static java.util.stream.Collectors.toList;

class GeneratePrograms {

    private static final double[] DEFAULT_PROBABILITIES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1, 1, 1, 1, 1};

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

            Program p = new Program("../programs/", 10000, Integer.parseInt(data[4]),
                    Integer.parseInt(data[5]), ForbidCycles.NONE, new double[]{1}, predicates, arities, variables,
                    constants);

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


    private static void generateAllPrograms() throws IOException {
        for (int numPredicates = 1; numPredicates < 5; numPredicates++) {
            //System.out.println("\n" + numPredicates);
            String[] predicates = new String[numPredicates];
            fillWithNames(predicates, "");
            for (int numAdditionalClauses = 0; numAdditionalClauses < 5; numAdditionalClauses++) {
                int maxNumClauses = predicates.length + numAdditionalClauses;
                for (int numVariables = 0; numVariables < 5; numVariables++) {
                    //System.out.print(".");
                    String[] variables = new String[numVariables];
                    fillWithNames(variables, "v");
                    for (int numConstants = 0; numConstants < 5; numConstants++) {
                        if (numConstants == 0 && numVariables == 0)
                            continue;
                        String[] constants = new String[numConstants];
                        fillWithNames(constants, "c");
                        for (int maxArity = 1; maxArity < 5; maxArity++) {
                            List<int[]> arities = generateArities(new int[0], predicates.length, maxArity);
                            for (int maxNumNodes = 1; maxNumNodes < 5; maxNumNodes++) {
                                String directory = "../programs/" + numPredicates + "p_" + numAdditionalClauses + "cl_" +
                                        numVariables + "v_" + numConstants + "co_" + maxNumNodes + "n_" +
                                        maxArity + "a/";
                                new File(directory).mkdir();
                                for (int[] localArities: arities) {
                                    Program p = new Program(directory, 1, maxNumNodes, maxNumClauses,
                                            ForbidCycles.NEGATIVE, DEFAULT_PROBABILITIES, predicates, localArities,
                                            variables, constants);
                                    long start = System.nanoTime();
                                    p.saveProgramsToFiles();
                                    long finish = System.nanoTime();
                                    System.out.println(directory + ", " + (finish - start));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        //generateAllPrograms();
        checkNumPrograms();

        /*Program p = new Program("../programs/", 1000, 4, 1,
                ForbidCycles.NEGATIVE, DEFAULT_PROBABILITIES, new String[]{"p"}, new int[]{4}, new String[]{"W", "X", "Y", "Z"},
                new String[]{"a"});
        p.saveProgramsToFiles();*/
    }
}
