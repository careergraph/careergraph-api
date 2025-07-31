package com.hcmute.careergraph.config.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


@Configuration
@EnableJpaRepositories(
        basePackages = "com.hcmute.careergraph.repository.sql",
        transactionManagerRef = "jpaTransactionManager"
)
public class JpaConfig {

    @Bean(name = "jpaTransactionManager")
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "jpaTransactionTemplate")
    @Primary
    public TransactionTemplate jpaTransactionTemplate(
            @Qualifier("jpaTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
