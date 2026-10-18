package com.fullcycle.notes.audit;
import com.fullcycle.notes.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_events")
public class AuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="actor_user_id") private AppUser actor;
 @Column(nullable=false,length=80) private String action;
 @Column(name="entity_type",nullable=false,length=80) private String entityType;
 @Column(name="entity_id",length=80) private String entityId;
 @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt=Instant.now();
 @Column(columnDefinition="TEXT") private String details;
 protected AuditEvent(){}
 public AuditEvent(AppUser actor,String action,String entityType,String entityId,String details){this.actor=actor;this.action=action;this.entityType=entityType;this.entityId=entityId;this.details=details;}
 public Long getId(){return id;} public AppUser getActor(){return actor;} public String getAction(){return action;}
 public String getEntityType(){return entityType;} public String getEntityId(){return entityId;} public Instant getOccurredAt(){return occurredAt;} public String getDetails(){return details;}
}