package br.com.sres.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 255)
    private String subject;
    @Column(length = 100)
    private String username;
    @Column(length = 255)
    private String email;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AccountStatus status;
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountEntity() { }

    public AccountEntity(UUID id, String subject, String username, String email, AccountStatus status, UUID planId, Instant createdAt, Instant updatedAt) {
        this.id = id; this.subject = subject; this.username = username; this.email = email;
        this.status = status; this.planId = planId; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public AccountStatus getStatus() { return status; }
    public UUID getPlanId() { return planId; }
    public void block() { status = AccountStatus.BLOCKED; updatedAt = Instant.now(); }
    public void unblock() { status = AccountStatus.ACTIVE; updatedAt = Instant.now(); }
    public void assignPlan(UUID planId) { this.planId = planId; updatedAt = Instant.now(); }
}
