package br.com.sres.accounts;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado da conta local.")
public enum AccountStatus { ACTIVE, BLOCKED }
