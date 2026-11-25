package com.pharoah.persistence.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reusable Generic In-Memory Repository implementation.
 * 
 * Demonstrates:
 * 1. Generic Class: <T, ID> type safety.
 * 2. Bounded Wildcards: <? extends T> for Covariance (PECS: Producer Extends).
 * 3. Bounded Wildcards: <? super T> for Contravariance.
 * 4. Generic Methods with custom type parameters <R>.
 */
public class InMemoryGenericRepository<T, ID> implements Repository<T, ID> {

    private final Map<ID, T> storage = new ConcurrentHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryGenericRepository(Function<T, ID> idExtractor) {
        if (idExtractor == null) {
            throw new IllegalArgumentException("idExtractor cannot be null.");
        }
        this.idExtractor = idExtractor;
    }

    @Override
    public void save(T entity) {
        if (entity != null) {
            ID id = idExtractor.apply(entity);
            storage.put(id, entity);
        }
    }

    /**
     * Demonstrates Bounded Wildcard (Producer Extends):
     * Accepts a collection of any subtype that extends T.
     */
    public void saveAll(Collection<? extends T> entities) {
        if (entities != null) {
            for (T entity : entities) {
                save(entity);
            }
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(ID id) {
        return storage.remove(id) != null;
    }

    @Override
    public long count() {
        return storage.size();
    }

    /**
     * Demonstrates Generic Method with Bounded Wildcards:
     * Transforms entities to any target type R using a mapper function.
     */
    public <R> List<R> mapAll(Function<? super T, ? extends R> mapper) {
        return storage.values().stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * Finds entities matching a generic predicate filter.
     */
    public List<T> findByCriteria(Predicate<? super T> predicate) {
        return storage.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
}
