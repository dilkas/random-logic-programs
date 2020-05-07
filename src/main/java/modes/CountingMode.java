package modes;

import model.Config;
import model.Program;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class CountingMode {

    public static void run() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(Config.PROGRAM_COUNTS_FILENAME));
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
            if (config.printProgramsWhileCounting)
                System.out.println("========================================");
            while (p.solve()) {
                i++;
                if (config.printProgramsWhileCounting) {
                    System.out.println("=====Program=====");
                    System.out.println(p.toString());
                }
            }
            if (i != predictedProgramCount) {
                System.out.println("Parameters: " + row);
                System.out.println("Number of programs: " + i);
            }
        }
        reader.close();
    }
}
