import java.util.HashSet;
import java.util.Set;

class Dependencies {

    private Set<Integer> dependencies;
    private boolean certain;

    Dependencies() {
        this.dependencies = new HashSet<>();
        this.certain = true;
    }

    void add(int e) {
        dependencies.add(e);
    }

    void setUncertain() {
        certain = false;
    }

    boolean areCertain() {
        return certain;
    }

    Set<Integer> getDependencies() {
        return dependencies;
    }
}
