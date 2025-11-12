package com.hcmute.careergraph.config.app;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * DataSeeder: on application start, checks if critical data exists and executes
 * the SQL seed script `init-scripts/mock-data.sql` if needed.
 *
 * Behavior:
 * - Tries to query `jobs` table count. If count > 0, seeding is skipped.
 * - If the table is missing or empty, it attempts to load the SQL script from
 *   classpath `init-scripts/mock-data.sql` first, then from filesystem
 *   `init-scripts/mock-data.sql` relative to the working directory.
 * - The script is executed via Spring's ResourceDatabasePopulator; errors
 *   continue execution so partial seeds do not abort the app startup.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;

    public DataSeeder(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean shouldSeed = false;

        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jobs", Integer.class);
            if (count == null || count == 0) {
                logger.info("Jobs table exists but is empty (count={}), will run seed script.", count);
                shouldSeed = true;
            } else {
                logger.info("Jobs table already has data (count={}), skipping seed.", count);
            }
        } catch (Exception ex) {
            // If the table doesn't exist or query fails, attempt to run seed
            logger.info("Could not query jobs table (it may not exist yet): {}", ex.getMessage());
            shouldSeed = true;
        }

        if (!shouldSeed) {
            return;
        }

        // Try classpath first, then filesystem relative path
        Resource resource = resourceLoader.getResource("classpath:init-scripts/mock-data.sql");
        if (!resource.exists()) {
            resource = resourceLoader.getResource("file:init-scripts/mock-data.sql");
        }

        if (!resource.exists()) {
            logger.warn("Seed file not found at 'classpath:init-scripts/mock-data.sql' or 'init-scripts/mock-data.sql'. Skipping data seeding.");
            return;
        }

        // Ensure created_date/last_modified_date have DB defaults so plain INSERTs without
        // those columns will succeed. This helps when the schema includes NOT NULL and
        // JPA populates timestamps on persist (but SQL inserts bypass JPA lifecycle).
        String[] alterDefaults = new String[] {
            "ALTER TABLE parties ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE parties ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE companies ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE companies ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidates ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidates ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE accounts ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE accounts ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE contacts ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE contacts ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE addresses ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE addresses ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE skills ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE skills ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_skill ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_skill ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE educations ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE educations ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_education ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_education ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_experience ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE candidate_experience ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE jobs ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE jobs ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE applications ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE applications ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE application_stage_history ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE application_stage_history ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE connections ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE connections ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP"
        };

        for (String alt : alterDefaults) {
            try {
                jdbcTemplate.execute(alt);
            } catch (Exception ex) {
                // Non-fatal: table or column may not exist in all versions; log and continue
                logger.debug("Could not apply alter default (ignored): {} -> {}", alt, ex.getMessage());
            }
        }

        // Read all SQL statements and execute them grouped by table in a deterministic order
        int successCount = 0;
        int failCount = 0;
        try (java.io.InputStream is = resource.getInputStream();
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {

            // 1) Collect statements
            java.util.List<String> statements = new java.util.ArrayList<>();
            {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append('\n');
                    if (trimmed.endsWith(";")) {
                        String stmt = sb.toString().trim();
                        if (stmt.endsWith(";")) {
                            stmt = stmt.substring(0, stmt.length() - 1);
                        }
                        statements.add(stmt);
                        sb.setLength(0);
                    }
                }
                if (sb.length() > 0) {
                    String trailing = sb.toString().trim();
                    if (!trailing.isEmpty()) {
                        statements.add(trailing);
                    }
                }
            }

            // 2) Group by table (works for INSERT INTO ... only)
            java.util.Map<String, java.util.List<String>> tableToStmts = new java.util.HashMap<>();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)^\\s*INSERT\\s+INTO\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b");
            for (String stmt : statements) {
                java.util.regex.Matcher m = p.matcher(stmt);
                if (m.find()) {
                    String table = m.group(1).toLowerCase();
                    tableToStmts.computeIfAbsent(table, k -> new java.util.ArrayList<>()).add(stmt);
                } else {
                    // Non-INSERT statements (if any) execute first
                    tableToStmts.computeIfAbsent("__misc__", k -> new java.util.ArrayList<>()).add(stmt);
                }
            }

            // 3) Deterministic execution order to respect foreign keys
            String[] order = new String[] {
                "__misc__",
                "parties",
                "companies",
                "candidates",
                "accounts",
                "contacts",
                "addresses",
                "skills",
                "candidate_skill",
                "educations",
                "candidate_education",
                "candidate_experience",
                "jobs",
                "applications",
                "application_stage_history",
                "connections"
            };

            for (String table : order) {
                java.util.List<String> stmts = tableToStmts.get(table);
                if (stmts == null || stmts.isEmpty()) continue;

                // Log pre-count if table is a real table
                if (!"__misc__".equals(table)) {
                    try {
                        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                        logger.info("Seeding table '{}' (before count={})", table, before);
                    } catch (Exception ignore) {
                        logger.info("Seeding table '{}' (count unavailable)", table);
                    }
                } else {
                    logger.info("Executing non-INSERT statements ({} stmts)", stmts.size());
                }

                for (String stmt : stmts) {
                    try {
                        logger.debug("Executing [{}]: {}", table, shorten(stmt, 800));
                        jdbcTemplate.execute(stmt);
                        successCount++;
                    } catch (Exception ex) {
                        failCount++;
                        logger.warn("Statement failed for table '{}'(continuing). Error: {}\nStatement: {}", table, ex.getMessage(), shorten(stmt, 1200));
                    }
                }

                if (!"__misc__".equals(table)) {
                    try {
                        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                        logger.info("Done table '{}' (after count={})", table, after);
                    } catch (Exception ex) {
                        logger.info("Done table '{}' (after count unavailable: {})", table, ex.getMessage());
                    }
                }
            }

            logger.info("Seed script executed with {} successful statements and {} failed statements.", successCount, failCount);

        } catch (Exception e) {
            logger.error("Failed to read/execute seed script: {}", e.getMessage(), e);
            // As a fallback, try ResourceDatabasePopulator to maximize compatibility
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
                populator.setContinueOnError(true);
                DatabasePopulatorUtils.execute(populator, dataSource);
                logger.info("Executed seed script with fallback populator: {}", resource.getDescription());
            } catch (Exception ex) {
                logger.error("Fallback populator also failed: {}", ex.getMessage(), ex);
            }
        }

        // Re-query job count to confirm seeding effect
        try {
            Integer newCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jobs", Integer.class);
            logger.info("Jobs table count after seeding: {}", newCount);
        } catch (Exception ex) {
            logger.warn("Could not query jobs table after seeding: {}", ex.getMessage());
        }
    }

    private String shorten(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...[truncated]";
    }
}