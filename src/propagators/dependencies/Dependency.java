package propagators.dependencies;

import java.util.Objects;

/** A dependency is either determined (in which case it only holds the predicate name) or undetermined (in which case
 * it also holds the source and target vertices of the edge that makes it undetermined). */
public final class Dependency {

    private final int predicate;
    private final Status status;
    private final int source;
    private final int target;

    public Dependency(int predicate, boolean determined) {
        this.predicate = predicate;
        if (determined) {
            status = Status.DETERMINED;
        } else {
            status = Status.UNDETERMINED;
        }
        source = -1;
        target = -1;
    }

    public Dependency(int predicate, int source, int target) {
        this.predicate = predicate;
        status = Status.ALMOST_DETERMINED;
        this.source = source;
        this.target = target;
    }

    public int getPredicate() {
        return predicate;
    }

    public Status getStatus() {
        return status;
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return predicate + " (" + status + "): " + source + "-" + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dependency other = (Dependency) o;
        return predicate == other.predicate && status == other.status && source == other.source &&
                target == other.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, status, source, target);
    }
}
