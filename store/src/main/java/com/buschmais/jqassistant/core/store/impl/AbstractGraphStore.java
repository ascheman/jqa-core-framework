package com.buschmais.jqassistant.core.store.impl;

import static com.buschmais.cdo.api.Query.Result;
import static com.buschmais.cdo.api.Query.Result.CompositeRowObject;

import java.util.Collection;
import java.util.Map;

import com.buschmais.cdo.api.Query;
import org.apache.commons.collections.map.LRUMap;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buschmais.cdo.api.CdoManager;
import com.buschmais.cdo.api.CdoManagerFactory;
import com.buschmais.cdo.api.ResultIterable;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.descriptor.Descriptor;
import com.buschmais.jqassistant.core.store.api.descriptor.FullQualifiedNameDescriptor;

/**
 * Abstract base implementation of a {@link Store}.
 * <p>
 * Provides methods for managing the life packages of a store, transactions,
 * resolving descriptors and executing CYPHER queries.
 * </p>
 */
public abstract class AbstractGraphStore implements Store {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGraphStore.class);
	private CdoManagerFactory cdoManagerFactory;
	private CdoManager cdoManager;
	private Map<String, FullQualifiedNameDescriptor> fqnCache;

	@Override
	public void start(Collection<Class<?>> types) {
		cdoManagerFactory = createCdoManagerFactory(types);
		cdoManager = cdoManagerFactory.createCdoManager();
		fqnCache = new LRUMap(65536);
	}

	@Override
	public void stop() {
		if (cdoManager != null) {
			cdoManager.close();
		}
		if (cdoManagerFactory != null) {
			closeCdoManagerFactory(cdoManagerFactory);
		}
	}

	@Override
	public <T extends Descriptor> T create(Class<T> type) {
		T descriptor = cdoManager.create(type);
		return descriptor;
	}

	@Override
	public <T extends FullQualifiedNameDescriptor> T create(Class<T> type, String fullQualifiedName) {
		T descriptor = cdoManager.create(type);
		descriptor.setFullQualifiedName(fullQualifiedName);
		fqnCache.put(fullQualifiedName, descriptor);
		return descriptor;
	}

	@Override
	public <T extends Descriptor, C extends T> C migrate(T descriptor, Class<C> concreteType) {
		if (descriptor instanceof FullQualifiedNameDescriptor) {
			fqnCache.remove(((FullQualifiedNameDescriptor) descriptor).getFullQualifiedName());
		}
		C migrated = cdoManager.migrate(descriptor, concreteType);
		if (migrated instanceof FullQualifiedNameDescriptor) {
			fqnCache.put(((FullQualifiedNameDescriptor) migrated).getFullQualifiedName(), (FullQualifiedNameDescriptor) migrated);
		}
		return migrated;
	}

	@Override
	public <T extends Descriptor> T find(Class<T> type, String fullQualifiedName) {
		T t = (T) fqnCache.get(fullQualifiedName);
		if (t != null) {
			return t;
		}
		ResultIterable<T> result = cdoManager.find(type, fullQualifiedName);
		return result.hasResult() ? result.getSingleResult() : null;
	}

	@Override
	public <QL> Result<CompositeRowObject> executeQuery(QL query, Map<String, Object> parameters) {
		return cdoManager.createQuery(query).withParameters(parameters).execute();
	}

	@Override
	public void reset() {
		if (LOGGER.isInfoEnabled()) LOGGER.info("Resetting store.");
        runQueryUntilResultIsZero("MATCH (n)-[r]-() WITH n, collect(r) as rels LIMIT 10000 FOREACH (r in rels | DELETE r) DELETE n RETURN COUNT(*) as deleted");
        runQueryUntilResultIsZero("MATCH (n) WITH n LIMIT 50000 DELETE n RETURN COUNT(*) as deleted");
        if (LOGGER.isInfoEnabled()) LOGGER.info("Reset finished.");
	}

    public interface DeletedCount {
        Long getDeleted();
    }

    private void runQueryUntilResultIsZero(String deleteNodesAndRels) {
        Query deleteNodesAndRelQuery = cdoManager.createQuery(deleteNodesAndRels, DeletedCount.class);
        DeletedCount result;
        do {
            beginTransaction();
            result = (DeletedCount)deleteNodesAndRelQuery.execute().getSingleResult();
            commitTransaction();
        } while (result.getDeleted() > 0);
    }

    @Override
	public void beginTransaction() {
		cdoManager.begin();
	}

	@Override
	public void commitTransaction() {
		cdoManager.commit();
	}

	@Override
	public void rollbackTransaction() {
		cdoManager.rollback();
	}

	public GraphDatabaseAPI getDatabaseService() {
		return getDatabaseAPI(cdoManager);
	}

	protected abstract GraphDatabaseAPI getDatabaseAPI(CdoManager cdoManager);

	/**
	 * Delegates to the sub class to start the database.
	 * 
	 * @return The {@link GraphDatabaseService} instance to use.
	 */
	protected abstract CdoManagerFactory createCdoManagerFactory(Collection<Class<?>> types);

	/**
	 * Delegates to the sub class to stop the database.
	 * 
	 * @param database
	 *            The used {@link GraphDatabaseService} instance.
	 */
	protected abstract void closeCdoManagerFactory(CdoManagerFactory database);

}
