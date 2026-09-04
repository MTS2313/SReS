package br.com.sres;

import br.com.sres.accounts.AccountService;
import br.com.sres.plans.PlanService;
import br.com.sres.usage.QuotaService;
import br.com.sres.storage.StorageService;
import br.com.sres.reports.ReportService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({AccountService.NotFoundException.class, PlanService.NotFoundException.class,
            QuotaService.NotFoundException.class})
    ProblemDetail notFound(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request); }

    @ExceptionHandler({PlanService.ConflictException.class, PlanService.AccountInactivePlanException.class,
            QuotaService.ConflictException.class, QuotaService.QuotaExceededException.class,
            QuotaService.BlockedAccountException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.CONFLICT, safeDetail(exception), request); }

    @ExceptionHandler(QuotaService.BadRequestException.class)
    ProblemDetail badRequest(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request); }

    @ExceptionHandler(StorageService.StorageException.class)
    ProblemDetail storageFailure(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.BAD_GATEWAY, "Falha ao processar armazenamento externo.", request); }

    @ExceptionHandler({ReportService.NotFoundException.class})
    ProblemDetail reportNotFound(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request); }

    @ExceptionHandler({ReportService.BadRequestException.class})
    ProblemDetail reportBadRequest(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request); }

    @ExceptionHandler({ReportService.BlockedAccountException.class})
    ProblemDetail reportBlocked(RuntimeException exception, HttpServletRequest request) { return problem(HttpStatus.CONFLICT, exception.getMessage(), request); }

    @ExceptionHandler({MethodArgumentNotValidException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ProblemDetail validation(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail resourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail internal(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição.", request);
    }

    private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail == null ? status.getReasonPhrase() : detail);
        problem.setType(java.net.URI.create("urn:sres:problem:" + status.value()));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        String correlationId = CorrelationIdFilter.current(request);
        if (correlationId != null) problem.setProperty("correlationId", correlationId);
        return problem;
    }

    private String safeDetail(RuntimeException exception) {
        return exception instanceof DataIntegrityViolationException ? "Conflito de integridade dos dados." : exception.getMessage();
    }
}
