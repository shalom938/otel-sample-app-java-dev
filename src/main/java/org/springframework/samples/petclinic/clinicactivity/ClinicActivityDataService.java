package org.springframework.samples.petclinic.clinicactivity;import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.ClinicActivityLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import javax.sql.DataSource;
import java.sql.Connection;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Random;@Servicepublic class ClinicActivityDataService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicActivityDataService.class);
    private static final int BATCH_SIZE = 1000;
    private static final int COPY_FLUSH_EVERY = 50_000;

    private final ClinicActivityLogRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    // List of 15 possible activity types
    private static final List<String> ACTIVITY_TYPES = List.of(
            "Patient Check-in", "Patient Check-out", "Appointment Scheduling", "Medical Record Update",
            "Prescription Issuance", "Lab Test Order", "Lab Test Result Review", "Billing Generation",
            "Payment Processing", "Inventory Check", "Staff Shift Start", "Staff Shift End",
            "Emergency Alert", "Consultation Note", "Follow-up Reminder"
    );
    private final Random random = new Random();@Autowired
    public ClinicActivityDataService(ClinicActivityLogRepository repository,
                                     JdbcTemplate jdbcTemplate,
                                     DataSource dataSource,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Transactional
    public int getActiveLogsRatio(String type) {
        if (type == null) {
            logger.warn("Received null type parameter in getActiveLogsRatio");
            return 0;
        }
        
        var all = repository.countLogsByType(type);
        if (all == 0) {
            logger.info("No logs found for type: {}, returning 0 ratio", type);
            return 0;
        }
        
        var active = repository.countActiveLogsByType(type);
        logger.debug("Calculating ratio for type: {}, active: {}, total: {}", type, active, all);
        return active/all;
    }@Transactional
    public void cleanupActivityLogs() {
        logger.info("Received request to clean up all clinic activity logs.");
        long startTime = System.currentTimeMillis();
        try {
            repository.deleteAllInBatch(); // Efficiently delete all entries
            long endTime = System.currentTimeMillis();
            logger.info("Successfully cleaned up all clinic activity logs in {} ms.", (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error during clinic activity log cleanup", e);
            throw new RuntimeException("Error cleaning up activity logs: " + e.getMessage(), e);
        }
    }@Transactional
    public void populateData(int totalEntries) {
        long startTime = System.currentTimeMillis();
        Connection con = null;
        try {
            con = DataSourceUtils.getConnection(dataSource);
            String databaseProductName = con.getMetaData().getDatabaseProductName();
            DataSourceUtils.releaseConnection(con, dataSource);
            con = null;if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
                logger.info("Using PostgreSQL COPY for data population of {} entries.", totalEntries);
                populateDataWithCopyToNewTransaction(totalEntries);
            } else {
                logger.info("Using JDBC batch inserts for data population of {} entries (Database: {}).", totalEntries, databaseProductName);
                populateDataWithJdbcBatchInNewTransaction(totalEntries);
            }
        } catch (Exception e) {
            logger.error("Error during data population orchestration", e);
            throw new RuntimeException("Error during data population orchestration: " + e.getMessage(), e);
        } finally {
            if (con != null) {
                 DataSourceUtils.releaseConnection(con, dataSource);
            }
        }
        long endTime = System.currentTimeMillis();logger.info("Finished data population for {} clinic activity logs in {} ms.", totalEntries, (endTime - startTime));
    }private void populateDataWithCopyToNewTransaction(int totalEntries) throws Exception {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            Faker faker = new Faker(new Locale("en-US"));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();for (int i = 0; i < totalEntries; i++) {
                String activityType = ACTIVITY_TYPES.get(random.nextInt(ACTIVITY_TYPES.size()));
                int numericVal = faker.number().numberBetween(1, 100_000);
                String ts = dtf.format(LocalDateTime.ofInstant(
                        faker.date().past(5 * 365, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault()));
                boolean statusFlag = faker.bool().bool();
                String payload = String.join(" ", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));

                sb.append(csv(activityType)).append(',')
                  .append(numericVal).append(',')
                  .append(csv(ts)).append(',')
                  .append(statusFlag).append(',')
                  .append(csv(payload)).append('\n');if ((i + 1) % COPY_FLUSH_EVERY == 0 || (i + 1) == totalEntries) {
                    copyManager.copyIn("COPY clinic_activity_logs (activity_type, numeric_value, event_timestamp, status_flag, payload) FROM STDIN WITH (FORMAT csv)", new java.io.StringReader(sb.toString()));
                    sb.setLength(0);
                    if (logger.isInfoEnabled()){
                        logger.info("COPY inserted {} / {} clinic activity logs...", (i + 1), totalEntries);
                    }
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }
            logger.error("Error during COPY data population with new transaction", e);
            throw e;
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }private void populateDataWithJdbcBatchInNewTransaction(int totalEntries) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            Faker faker = new Faker(new Locale("en-US"));
            String sql = "INSERT INTO clinic_activity_logs (activity_type, numeric_value, event_timestamp, status_flag, payload) VALUES (?, ?, ?, ?, ?)";
            for (int i = 0; i < totalEntries; ) {
                List<Object[]> batchArgs = new ArrayList<>();
                for (int j = 0; j < BATCH_SIZE && i < totalEntries; j++, i++) {
                    String activityType = ACTIVITY_TYPES.get(random.nextInt(ACTIVITY_TYPES.size()));
                    int numericVal = faker.number().numberBetween(1, 100_000);
                    Timestamp eventTimestamp = Timestamp.from(faker.date().past(5 * 365, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toInstant()
                    );
                    boolean statusFlag = faker.bool().bool();
                    String payload = String.join(" ", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));
                    batchArgs.add(new Object[]{activityType, numericVal, eventTimestamp, statusFlag, payload});
                }
                if (!batchArgs.isEmpty()) {
                    jdbcTemplate.batchUpdate(sql, batchArgs);
                     if (logger.isInfoEnabled()) {
                        logger.info("JDBC batch inserted {} / {} clinic activity logs...", i, totalEntries);
                     }
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }logger.error("Error during JDBC batch population with new transaction", e);
            throw new RuntimeException("Error during JDBC batch population with new transaction: " + e.getMessage(), e);
        }
    }private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"").replace("\\", "\\\\");
        return '"' + escaped + '"';
    }
}