package propagators;

import java.util.Objects;

/** A dependency is either determined (in which case it only holds the predicate name) or undetermined (in which case
 * it also holds the source and target vertices of the edge that makes it undetermined). */
final class Dependency {

    private final int predicate;
    private final boolean determined;
    private final int source;
    private final int target;

    Dependency(int predicate) {
        this.predicate = predicate;
        determined = true;
        source = 0;
        target = 0;
    }

    Dependency(int predicate, int source, int target) {
        this.predicate = predicate;
        determined = false;
        this.source = source;
        this.target = target;
    }

    int getPredicate() {
        return predicate;
    }

    boolean isDetermined() {
        return determined;
    }

    int getSource() {
        return source;
    }

    int getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return predicate + " (" + determined + "): " + source + "-" + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dependency other = (Dependency) o;
        return predicate == other.predicate && determined == other.determined && source == other.source &&
                target == other.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, determined, source, target);
    }
}
