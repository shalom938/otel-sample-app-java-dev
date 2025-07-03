package org.springframework.samples.petclinic.monitoring;

import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private final ReentrantLock threadLock = new ReentrantLock();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread monitorThread;

    public MonitorService() {
        initializeMonitorThread();
    }

    private void initializeMonitorThread() {
        threadLock.lock();
        try {
            if (monitorThread != null && monitorThread.isAlive()) {
                throw new IllegalStateException("Monitor thread is already running");
            }

            if (isRunning.get()) {
                throw new IllegalStateException("Monitor service is already initialized");
            }

            monitorThread = new Thread(this::monitoringTask, "MonitorThread");
            monitorThread.setDaemon(true);
            isRunning.set(true);
            monitorThread.start();
            logger.info("Monitor thread initialized successfully");
        } finally {
            threadLock.unlock();
        }
    }

    private void monitoringTask() {
        while (isRunning.get()) {
            try {
                // Perform monitoring tasks here
                Thread.sleep(5000); // Sleep for 5 seconds between checks
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Monitor thread interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("Error in monitoring task", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        threadLock.lock();
        try {
            isRunning.set(false);
            if (monitorThread != null) {
                monitorThread.interrupt();
                try {
                    monitorThread.join(5000); // Wait up to 5 seconds for thread to terminate
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while shutting down monitor thread", e);
                }
            }
        } finally {
            threadLock.unlock();
        }
    }
}