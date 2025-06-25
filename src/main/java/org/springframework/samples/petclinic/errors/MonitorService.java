package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Componentpublic class MonitorService implements SmartLifecycle {

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
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					Logger.error("Monitoring thread interrupted", e);
					throw new RuntimeException(e);
				}
				Span span = otelTracer.spanBuilder("monitor").startSpan();

				try {
					Logger.info("Executing system health check...");
					monitor();
					Metrics.counter("system.health.check.success").increment();
				} catch (Exception e) {
					Logger.error("System health check failed", e);
					Metrics.counter("system.health.check.failure").increment();
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		Logger.info("System health monitoring service started.");
	}private void monitor() {
		try {
			// Check CPU usage
			Double cpuUsage = SystemMetrics.getCPUUsage();
			// Check memory usage
			Long memoryUsage = SystemMetrics.getMemoryUsage();
			// Check disk space
			Long diskSpace = SystemMetrics.getDiskSpace();

			// Log system health metrics
			Logger.info("System Health Metrics - CPU: {}%, Memory: {} MB, Disk: {} GB",
				cpuUsage, memoryUsage/1024/1024, diskSpace/1024/1024/1024);

			// Alert if metrics exceed thresholds
			if (cpuUsage > 90 || memoryUsage > 0.9 * Runtime.getRuntime().maxMemory()) {
				Logger.warn("System resources reaching critical levels");
			}
		} catch (Exception e) {
			Logger.error("Error monitoring system health: {}", e.getMessage());
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
		return false;
	}
}