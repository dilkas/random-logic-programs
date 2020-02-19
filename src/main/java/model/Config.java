package model;

import java.util.List;

class Config {
    private List<IndependentPair> independentPairs;

    List<IndependentPair> getIndependentPairs() {
        return independentPairs;
    }

    Config(List<IndependentPair> independentPairs) {
        this.independentPairs = independentPairs;
    }
}
