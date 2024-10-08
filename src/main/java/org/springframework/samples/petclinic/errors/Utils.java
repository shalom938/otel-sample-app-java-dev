package org.springframework.samples.petclinic.errors;

import org.apache.coyote.BadRequestException;

public class Utils {
	public static void ThrowIllegalStateException() throws IllegalStateException {
		throw new IllegalStateException("Some unexpected state error");
	}

	public static void ThrowBadRequestException() throws BadRequestException {
		throw new BadRequestException("validation failure");
	}

	public static void ThrowUnsupportedOperationException() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("operation is not supported");
	}

	public static <T extends Exception> void throwException(Class<T> exceptionClass, String message) throws T {
		try {
			// Use reflection to create a new instance of the exception with the message
			T exceptionInstance = exceptionClass.getConstructor(String.class).newInstance(message);
			throw exceptionInstance;
		} catch (ReflectiveOperationException e) {
			// Handle case where the exception type doesn't have the expected constructor
			throw new IllegalArgumentException("Invalid exception type provided", e);
		}
	}
}
