package src.services;

import java.util.List;

public interface BaseService<T> {
    List<T> getAll();
    T getById(int id);
    boolean add(T item);
    boolean update(T item);
    boolean delete(int id);
    void save();
    void load();
} 