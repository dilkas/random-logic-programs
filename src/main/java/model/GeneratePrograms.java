package model;
import java.io.*;

import modes.CountingMode;
import modes.ExperimentalMode;
import modes.NormalMode;

class GeneratePrograms {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("The generator has three modes: normal, count, experiment.");
            return;
        }
        switch (args[0]) {
            case "count":
                CountingMode.run();
                break;
            case "experiment":
                ExperimentalMode.run();
                break;
            case "normal":
                NormalMode.run();
                break;
            default:
                System.out.println("The generator has three modes: normal, count, experiment.");
        }
    }
}
