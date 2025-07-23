package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Component/**
 * Service responsible for system monitoring and metrics collection.
 * Implements SmartLifecycle for proper Spring lifecycle management.
 */
public class MonitorService implements SmartLifecycle {

    private boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");

        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                Span span = otelTracer.spanBuilder("system.monitor").startSpan();
                
                try {
                    Thread.sleep(5000);
                    System.out.println("Background monitoring service is running...");
                    monitor();
                    
                    span.setAttribute("monitoring.status", "success");
                    span.setStatus(StatusCode.OK);
                    
                } catch (InterruptedException e) {
                    span.setAttribute("monitoring.status", "interrupted");
                    span.setStatus(StatusCode.ERROR);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    span.recordException(e);
                    span.setAttribute("monitoring.status", "error");
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setDaemon(true);
        backgroundThread.start();
        System.out.println("Background monitoring service started.");
    }
    /**
     * Monitors the system resources and performs necessary checks.
     * This method runs in a background thread and continuously monitors
     * until the service is stopped.
     *
     * @throws InvalidPropertiesFormatException if monitoring configuration is invalid
     */
    private void monitor() throws InvalidPropertiesFormatException {
        while (running) {
            try {
                // Perform monitoring tasks
                checkSystemResources();
                checkApplicationHealth();
                
                // Sleep for the configured interval
                Thread.sleep(monitoringInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log any monitoring errors
                System.err.println("Monitoring error: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        // Stop the background task
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running && backgroundThread != null && backgroundThread.isAlive();
    }
}