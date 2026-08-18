package fi.vm.sade.valinta.sharedutils;

import java.security.Principal;
import java.util.Objects;

public class SimplePrincipal implements Principal {
    private final String name;

    public SimplePrincipal(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimplePrincipal that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
