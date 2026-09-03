package br.com.sres;

import br.com.sres.accounts.AccountService;
import br.com.sres.plans.PlanService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({AccountService.NotFoundException.class, PlanService.NotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) { return problem(HttpStatus.NOT_FOUND, exception.getMessage()); }

    @ExceptionHandler({PlanService.ConflictException.class, PlanService.AccountInactivePlanException.class,
            DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception) { return problem(HttpStatus.CONFLICT, exception.getMessage()); }

    private ProblemDetail problem(HttpStatus status, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
