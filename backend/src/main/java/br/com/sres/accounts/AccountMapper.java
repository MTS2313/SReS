package br.com.sres.accounts;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "plan", ignore = true)
    AccountResponse toResponse(AccountEntity account);
}
