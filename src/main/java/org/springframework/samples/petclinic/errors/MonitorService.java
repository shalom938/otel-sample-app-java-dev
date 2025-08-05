package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Servicepackage org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Service
public class MonitorService implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private final Tracer tracer;
    private volatile boolean running = false;
    private Thread backgroundThread;

    public MonitorService(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Span span = tracer.spanBuilder("monitor").startSpan();
                    try {
                        // Actual monitoring logic here instead of throwing test exception
                        logger.debug("Monitoring service running...");
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        span.recordException(e);
                        logger.error("Monitor operation failed", e);
                    } finally {
                        span.end();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        backgroundThread.start();
        logger.info("Monitor service started");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join(5000);
                if (backgroundThread.isAlive()) {
                    backgroundThread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to stop monitor service", e);
            }
        }
        logger.info("Monitor service stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @PreDestroy
    public void destroy() {
        stop();
    }
}    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join(5000);
                if (backgroundThread.isAlive()) {
                    backgroundThread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to stop monitor service", e);
            }
        }
        logger.info("Monitor service stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @PreDestroy
    public void destroy() {
        stop();
    }