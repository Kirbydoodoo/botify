package net.robinfriedli.botify.interceptors;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public abstract class CollectingInterceptor extends ChainableInterceptor {

    private final List<Object> createdEntities = Lists.newArrayList();
    private final List<Object> deletedEntities = Lists.newArrayList();
    private final List<Object> updatedEntities = Lists.newArrayList();

    public CollectingInterceptor(Interceptor next, Logger logger) {
        super(next, logger);
    }

    public abstract void afterCommit();

    @Override
    public void onSaveChained(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        createdEntities.add(entity);
    }

    @Override
    public void onDeleteChained(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        deletedEntities.add(entity);
    }

    @Override
    public void onFlushDirtyChained(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        updatedEntities.add(entity);
    }

    @Override
    public void afterTransactionCompletionChained(Transaction tx) {
        if (!tx.getRollbackOnly()) {
            afterCommit();
        }
        createdEntities.clear();
        deletedEntities.clear();
        updatedEntities.clear();
    }

    public List<Object> getCreatedEntities() {
        return createdEntities;
    }

    public <E> List<E> getCreatedEntities(Class<E> type) {
        return createdEntities.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    public List<Object> getDeletedEntities() {
        return deletedEntities;
    }

    public <E> List<E> getDeletedEntities(Class<E> type) {
        return deletedEntities.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    public List<Object> getUpdatedEntities() {
        return updatedEntities;
    }

    public <E> List<E> getUpdatedEntities(Class<E> type) {
        return updatedEntities.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    public List<Object> getAffectedEntites() {
        Iterable<Object> concat = Iterables.concat(createdEntities, deletedEntities, updatedEntities);

        return StreamSupport.stream(concat.spliterator(), false).collect(Collectors.toList());
    }

    public <E> List<E> getAffectedEntities(Class<E> type) {
        Iterable<Object> concat = Iterables.concat(createdEntities, deletedEntities, updatedEntities);

        return StreamSupport.stream(concat.spliterator(), false)
            .filter(type::isInstance)
            .map(type::cast)
            .collect(Collectors.toList());
    }

}
