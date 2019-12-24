import java.util.List;

class PotentialPredicate {

    private boolean[] compatible;
    private boolean determined;

    PotentialPredicate(List<Integer> domainValues, List<Integer> predicatesOfInterest) {
        compatible = new boolean[predicatesOfInterest.size()];
        determined = domainValues.size() == 1;
        for (int value : domainValues) {
            int index = predicatesOfInterest.indexOf(value);
            if (index > -1)
                compatible[index] = true;
        }
    }
}
