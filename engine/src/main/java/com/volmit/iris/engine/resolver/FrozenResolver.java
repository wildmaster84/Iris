package com.volmit.iris.engine.resolver;

import art.arcane.amulet.format.Form;
import com.volmit.iris.platform.PlatformNamespaceKey;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class FrozenResolver<T extends Resolvable> implements Resolver<T> {
    private final Map<String, T> registry;
    private final String namespace;

    public FrozenResolver(String namespace, Map<String, T> registry)
    {
        this.registry = Collections.unmodifiableMap(registry);
        this.namespace = namespace;
    }

    @Override
    public boolean hasNamespace(String namespace) {
        return this.namespace.equals(namespace);
    }

    public T resolve(PlatformNamespaceKey key) {
        return registry.get(key.getKey());
    }

    @Override
    public T resolve(String key) {
        return registry.get(key);
    }

    @Override
    public void print(String type, Object printer, int indent) {
        printer.i(Form.repeat(" ", indent) + "Frozen[" + namespace + "] " + type);
    }

    @Override
    public Resolver<T> and(String namespace, Resolver<T> resolver) {
        if(!namespace.equals(getNamespace())) {
            return new CompositeResolver<>(Map.of(namespace, resolver, getNamespace(), this));
        }

        return new MergedNamespaceResolver<>(namespace, this, resolver);
    }
}
