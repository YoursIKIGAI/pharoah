package com.pharoah.persistence.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic Repository interface demonstrating Java Generics:
 * 1. Type parameters <T, ID> for compile-time type safety.
 * 2. Elimination of manual type casting.
 * 3. Contract for generic CRUD persistence operations.
 *
 * @param <T>  The domain entity type.
 * @param <ID> The primary key identifier type (e.g. String, Long).
 */
public interface Repository<T, ID> {

    void save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean deleteById(ID id);

    long count();
}
