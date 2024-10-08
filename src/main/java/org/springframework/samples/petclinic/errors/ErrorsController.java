package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Errors")
public class ErrorsController implements InitializingBean {

	@Autowired
	private AsyncService asyncService;

	@Autowired
	private OpenTelemetry openTelemetry;

	private Tracer otelTracer;

	private ErrorsService errorsService;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("SampleInsightsController");
		this.errorsService = new ErrorsService(this.otelTracer);
	}

	//generate unhandled exception of type IllegalStateException
	@GetMapping("error1")
	public void error1() throws IllegalStateException {
		Utils.throwException(IllegalStateException.class,"some message");
	}

	//handled exception of type IllegalStateException
	@GetMapping("error2")
	public void error2() {
		errorsService.handleException(IllegalStateException.class,"some message");
	}

	//generate unhandled exception of type UnsupportedOperationException
	@GetMapping("error3")
	public void error3(){
		Utils.ThrowUnsupportedOperationException();
	}

	//handled exception of type UnsupportedOperationException
	@GetMapping("error4")
	public void error4() {
		errorsService.handleUnsupportedOperationException();
	}

	@GetMapping("error5")
	public void error5() throws BadRequestException {
		Utils.ThrowBadRequestException();
	}

	//ThrowBadRequestException

	@GetMapping("/run-async")
	public void runAsyncTask() {
		asyncService.runMultipleAsyncTasks();
	}

}
