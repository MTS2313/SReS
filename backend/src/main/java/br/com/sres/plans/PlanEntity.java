package br.com.sres.plans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(name = "weekly_limit", nullable = false) private int weeklyLimit;
    @Column(nullable = false) private boolean active;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected PlanEntity() { }
    public PlanEntity(UUID id, String name, int weeklyLimit, boolean active, boolean isDefault, Instant createdAt, Instant updatedAt) { this.id=id;this.name=name;this.weeklyLimit=weeklyLimit;this.active=active;this.isDefault=isDefault;this.createdAt=createdAt;this.updatedAt=updatedAt; }
    public UUID getId(){return id;} public String getName(){return name;} public int getWeeklyLimit(){return weeklyLimit;} public boolean isActive(){return active;} public boolean isDefault(){return isDefault;}
    public void update(String name, Integer weeklyLimit){if(name!=null)this.name=name;if(weeklyLimit!=null)this.weeklyLimit=weeklyLimit;updatedAt=Instant.now();}
    public void activate(){active=true;updatedAt=Instant.now();} public void deactivate(){active=false;updatedAt=Instant.now();} public void setDefault(boolean value){isDefault=value;updatedAt=Instant.now();}
}
