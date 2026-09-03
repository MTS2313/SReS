package br.com.sres.plans;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlanService {
    private final PlanRepository plans; private final PlanMapper mapper; private final JdbcTemplate jdbc;
    public PlanService(PlanRepository plans, PlanMapper mapper, JdbcTemplate jdbc){this.plans=plans;this.mapper=mapper;this.jdbc=jdbc;}
    public List<PlanResponse> all(){return plans.findAllByOrderByNameAsc().stream().map(mapper::toResponse).toList();}
    public PlanResponse find(UUID id){return mapper.toResponse(plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado")));}
    public PlanResponse defaultPlan(){return mapper.toResponse(plans.findByIsDefaultTrue().orElseThrow(() -> new IllegalStateException("Plano padrão não configurado")));}
    public PlanEntity requireDefaultEntity(){return plans.findByIsDefaultTrue().orElseThrow(() -> new IllegalStateException("Plano padrão não configurado"));}
    public PlanEntity requireActive(UUID id){var plan=plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));if(!plan.isActive())throw new AccountInactivePlanException();return plan;}
    @Transactional public PlanResponse create(PlanRequest request, String actor){var plan=new PlanEntity(UUID.randomUUID(),request.name(),request.weeklyLimit(),request.active()==null||request.active(),false,java.time.Instant.now(),java.time.Instant.now());plan=plans.save(plan);return mapper.toResponse(plan);}
    @Transactional public PlanResponse update(UUID id, PlanUpdateRequest request){var plan=plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));plan.update(request.name(),request.weeklyLimit());return mapper.toResponse(plans.save(plan));}
    @Transactional public PlanResponse activate(UUID id){var plan=plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));plan.activate();return mapper.toResponse(plans.save(plan));}
    @Transactional public PlanResponse deactivate(UUID id){var plan=plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));if(plan.isDefault())throw new ConflictException("Plano padrão não pode ser inativado");plan.deactivate();return mapper.toResponse(plans.save(plan));}
    @Transactional public PlanResponse makeDefault(UUID id){var plan=plans.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));if(!plan.isActive())throw new AccountInactivePlanException();jdbc.update("update plans set is_default = false, updated_at = current_timestamp where is_default = true");plan.setDefault(true);return mapper.toResponse(plans.save(plan));}
    public static class NotFoundException extends RuntimeException { public NotFoundException(String message){super(message);} }
    public static class ConflictException extends RuntimeException { public ConflictException(String message){super(message);} }
    public static class AccountInactivePlanException extends RuntimeException { public AccountInactivePlanException(){super("Plano inativo não pode ser atribuído");} }
}
