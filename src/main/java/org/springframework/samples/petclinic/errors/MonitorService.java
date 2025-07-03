package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Componentpublic class MonitorService implements SmartLifecycle {

	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread backgroundThread;
	private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
	@Autowired
	private OpenTelemetry openTelemetry;

	@Override
	public synchronized void start() {
		if (running.get()) {
			logger.warn("Monitor service is already running");
			return;
		}

		var otelTracer = openTelemetry.getTracer("MonitorService");
		running.set(true);
		backgroundThread = new Thread(() -> {
			while (running.get()) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					logger.warn("Monitor thread interrupted", e);
					Thread.currentThread().interrupt();
					return;
				}
				Span span = otelTracer.spanBuilder("monitor").startSpan();

				try {
					logger.debug("Background service is running...");
					monitor();
				} catch (Exception e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		logger.info("Background service started.");
	}private void monitor() {
		try {
			Utils.throwException(IllegalStateException.class,"monitor failure");
		} catch (InvalidPropertiesFormatException e) {
			logger.error("Monitor operation failed", e);
			throw new RuntimeException("Monitor operation failed", e);
		} catch (Exception e) {
			logger.error("Unexpected error in monitor operation", e);
			throw new RuntimeException("Unexpected error in monitor operation", e);
		}
	}

	@Override
	public void stop() {
		logger.info("Stopping background service...");
		// Stop the background task
		running.set(false);
		if (backgroundThread != null) {
			try {
				backgroundThread.join(); // Wait for the thread to finish
				logger.info("Background thread stopped successfully");
			} catch (InterruptedException e) {
				logger.warn("Thread interruption while stopping service", e);
				Thread.currentThread().interrupt();
			}
		}
		logger.info("Background service stopped.");
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}
}