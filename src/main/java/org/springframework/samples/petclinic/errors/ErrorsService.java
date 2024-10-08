package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class ErrorsService
{
	private final Tracer otelTracer;
	public ErrorsService(Tracer otelTracer)
	{
		this.otelTracer = otelTracer;
	}
	public <T extends Exception> void handleException(Class<T> exceptionClass, String message) {
		Span span = otelTracer.spanBuilder("handleException").startSpan();

		try {
			Utils.throwException(exceptionClass, message);
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}

	public void handleUnsupportedOperationException()
	{
		Span span = otelTracer.spanBuilder("handleUnsupportedOperationException").startSpan();

		try {
			Utils.ThrowUnsupportedOperationException();
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}
}
