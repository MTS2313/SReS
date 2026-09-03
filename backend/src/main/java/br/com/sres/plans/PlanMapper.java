package br.com.sres.plans;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanMapper {
    @Mapping(target = "isDefault", source = "default")
    PlanResponse toResponse(PlanEntity plan);
}
