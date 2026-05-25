package com.hcmute.careergraph.config.app;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.stereotype.Component;

/**
 * DataSeeder: on application start, checks if critical data exists and executes
 * a SQL seed script if needed.
 *
 * Behavior:
 * - Checks a configured table row count.
 * - If row count is less than configured minimum, it executes a configured
 * seed SQL script.
 * - Script path is resolved in this order:
 * classpath:<path>, file:<path>, file:/app/<path>,
 * classpath:init-scripts/mock-data.sql,
 * file:init-scripts/mock-data.sql
 * - SQL execution is fail-fast (continueOnError=false).
 * - If a statement fails, logger prints line number + failed SQL statement.
 */
@Component
@Order(0)
public class DataSeeder implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.script-path:init-scripts/seed-basic.sql}")
    private String seedScriptPath;

    @Value("${app.seed.check-table:jobs}")
    private String checkTable;

    @Value("${app.seed.min-row-count:1}")
    private int minRowCount;

    public DataSeeder(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!seedEnabled) {
            logger.info("Data seed disabled by config (app.seed.enabled=false).");
            return;
        }

        boolean shouldSeed = true;

        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + checkTable, Integer.class);
            if (count == null || count < minRowCount) {
                logger.info("Table '{}' has count={} (< {}), will run seed script.", checkTable, count, minRowCount);
            } else {
                logger.info("Table '{}' already has enough data (count={} >= {}), skipping seed.", checkTable, count,
                        minRowCount);
                shouldSeed = false;
            }
        } catch (Exception ex) {
            logger.info("Could not query table '{}' yet ({}), will skip this seed run.", checkTable, ex.getMessage());
            shouldSeed = false;
        }

        if (!shouldSeed) {
            return;
        }

        Resource resource = resolveSeedResource();

        if (!resource.exists()) {
            logger.warn("Seed file not found (app.seed.script-path='{}'). Skipping data seeding.", seedScriptPath);
            return;
        }

        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
            // Fail-fast to avoid partial data state when SQL script has invalid statements.
            populator.setContinueOnError(false);
            populator.setIgnoreFailedDrops(true);
            DatabasePopulatorUtils.execute(populator, dataSource);
            logger.info("Seed script executed: {}", resource.getDescription());
        } catch (ScriptStatementFailedException ex) {
            logger.error(
                    "Seed failed. Script: {}. Root cause: {}",
                    seedScriptPath,
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                    ex);
            throw ex;
        } catch (ScriptException ex) {
            ScriptStatementFailedException detailed = findStatementFailure(ex);
            if (detailed != null) {
                logger.error(
                        "Seed failed. Script: {}. Root cause: {}",
                        seedScriptPath,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                        ex);
            } else {
                logger.error("Failed to execute seed script '{}': {}", seedScriptPath, ex.getMessage(), ex);
            }
            throw ex;
        } catch (Exception ex) {
            ScriptStatementFailedException detailed = findStatementFailure(ex);
            if (detailed != null) {
                logger.error(
                        "Seed failed. Script: {}. Root cause: {}",
                        seedScriptPath,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                        ex);
            } else {
                logger.error("Failed to execute seed script '{}': {}", seedScriptPath, ex.getMessage(), ex);
            }
            throw ex;
        }

        try {
            Integer newCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + checkTable, Integer.class);
            logger.info("Table '{}' count after seeding: {}", checkTable, newCount);
        } catch (Exception ex) {
            logger.warn("Could not query '{}' after seeding: {}", checkTable, ex.getMessage());
        }
    }

    private Resource resolveSeedResource() {
        String[] candidates = new String[] {
                "classpath:" + seedScriptPath,
                "file:" + seedScriptPath,
                "file:/app/" + seedScriptPath,
                "classpath:init-scripts/mock-data.sql",
                "file:init-scripts/mock-data.sql"
        };

        for (String candidate : candidates) {
            Resource resource = resourceLoader.getResource(candidate);
            if (resource.exists()) {
                logger.info("Resolved seed script from: {}", candidate);
                return resource;
            }
        }

        return resourceLoader.getResource("classpath:missing.sql");
    }

    private ScriptStatementFailedException findStatementFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ScriptStatementFailedException failed) {
                return failed;
            }
            current = current.getCause();
        }
        return null;
    }
}