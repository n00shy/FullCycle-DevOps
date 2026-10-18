package com.fullcycle.notes.audit;
import com.fullcycle.notes.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuditService {
 private final AuditEventRepository events;
 public AuditService(AuditEventRepository events){this.events=events;}
 @Transactional public void record(AppUser actor,String action,String entityType,Long entityId,String details){
  events.save(new AuditEvent(actor,action,entityType,entityId==null?null:entityId.toString(),details));
 }
}