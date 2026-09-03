package br.com.sres.plans;

import jakarta.validation.constraints.Min;

public record PlanUpdateRequest(String name, @Min(1) Integer weeklyLimit) { }
