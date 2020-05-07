package modes;

import model.Config;
import model.Program;

import java.io.IOException;

public class NormalMode {

    public static void run() throws IOException {
        Program p = new Program(Config.initialiseFromFile());
        p.saveProgramsToFiles();
    }
}
