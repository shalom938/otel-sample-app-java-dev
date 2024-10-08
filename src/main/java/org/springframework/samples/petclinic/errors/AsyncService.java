package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

	private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
	//@Autowired
	//private OpenTelemetry openTelemetry;
	private static final Tracer tracer = GlobalOpenTelemetry.getTracer("AsyncService");


	@Async
	private void runAsyncTask(int iteration, Tracer otelTracer) {
		logger.info("Starting async task #" + iteration);
		Span span = otelTracer.spanBuilder("entry_"+iteration)
			.setNoParent().startSpan();

		try {

			Utils.ThrowBadRequestException();
			// Simulate a long-running task
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (BadRequestException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
		logger.info("Async task #" + iteration + " completed.");
	}

	public void runMultipleAsyncTasks() {
		//var otelTracer = openTelemetry.getTracer("runAsyncTask");
		for (int i = 1; i <= 5; i++) {
			runAsyncTask(i, tracer); // Trigger 5 asynchronous tasks
		}
	}

}
