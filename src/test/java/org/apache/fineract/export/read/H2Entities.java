package org.apache.fineract.export.read;

import org.apache.fineract.export.ExportJob;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.Variable;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * EclipseLink on an in-memory H2 database (MySQL mode) with only the payment and export job entities.
 */
public final class H2Entities {

    private H2Entities() {
    }

    public static EntityManagerFactory create(String name) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1");

        MutablePersistenceUnitInfo unit = new MutablePersistenceUnitInfo();
        unit.setPersistenceUnitName(name);
        unit.setPersistenceUnitRootUrl(H2Entities.class.getProtectionDomain().getCodeSource().getLocation());
        unit.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
        unit.setNonJtaDataSource(dataSource);
        unit.setExcludeUnlistedClasses(true);
        unit.addManagedClassName(AbstractPersistableCustom.class.getName());
        unit.addManagedClassName(Transfer.class.getName());
        unit.addManagedClassName(TransactionRequest.class.getName());
        unit.addManagedClassName(Variable.class.getName());
        unit.addManagedClassName(ExportJob.class.getName());

        Map<String, Object> properties = new HashMap<>();
        properties.put("eclipselink.weaving", "false");
        properties.put("eclipselink.ddl-generation", "create-tables");
        properties.put("eclipselink.ddl-generation.output-mode", "database");
        properties.put("eclipselink.cache.shared.default", "false");
        properties.put("eclipselink.logging.level", "SEVERE");
        return new PersistenceProvider().createContainerEntityManagerFactory(unit, properties);
    }

    public static void inTransaction(EntityManagerFactory factory, Consumer<EntityManager> work) {
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            work.accept(em);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
