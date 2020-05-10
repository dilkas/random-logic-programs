package modes;

import model.Config;
import model.Program;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class NormalMode {

    public static void run() throws IOException {
        Program p = new Program(Config.initialiseFromFile());
        Solver solver = p.model.getSolver();
        solver.setGeometricalRestart(10, 1.1, new FailCounter(p.model, 1), 100);
        if (p.config.timeout != null)
            solver.limitTime(p.config.timeout);
        if (p.config.printDebugInfo) {
            solver.showDecisions();
            solver.showContradiction();
        }

        for (int i = 0; i < p.config.numSolutions && solver.solve(); i++) {
            String suffix = "";
            if (p.config.numSolutions > 1)
                suffix = "_" + i;
            BufferedWriter writer = new BufferedWriter(new FileWriter(p.config.outputDirectory +
                    p.config.prefix + suffix +".pl"));
            writer.write(p.toString());
            writer.close();
        }
    }
}
