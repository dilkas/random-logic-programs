package model;

import java.util.List;

class Config {
    private List<PredicatePair> independentPairs;

    List<PredicatePair> getIndependentPairs() {
        return independentPairs;
    }

    Config(List<PredicatePair> independentPairs) {
        this.independentPairs = independentPairs;
    }
}
