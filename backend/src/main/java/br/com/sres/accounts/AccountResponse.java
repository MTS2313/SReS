package br.com.sres.accounts;

import br.com.sres.plans.PlanResponse;

import java.util.UUID;

public record AccountResponse(UUID id, String subject, String username, String email, AccountStatus status, PlanResponse plan) { }
