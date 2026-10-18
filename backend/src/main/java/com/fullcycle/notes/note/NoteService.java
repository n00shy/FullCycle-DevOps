package com.fullcycle.notes.note;
import com.fullcycle.notes.audit.AuditService;
import com.fullcycle.notes.user.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
@Service
public class NoteService {
 private final NoteRepository notes; private final UserRepository users; private final AuditService audit;
 public NoteService(NoteRepository notes,UserRepository users,AuditService audit){this.notes=notes;this.users=users;this.audit=audit;}
 @Transactional(readOnly=true) public Page<NoteResponse> list(Long userId,String q,String category,int page,int size){
  Pageable pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"updatedAt").and(Sort.by(Sort.Direction.DESC,"id")));
  return notes.searchOwned(userId,clean(category),clean(q),pageable).map(NoteResponse::from);
 }
 @Transactional public NoteResponse create(Long userId,NoteRequest r){
  AppUser owner=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  Note n=notes.save(new Note(owner,r.title().trim(),r.content()==null?"":r.content(),clean(r.category())));
  audit.record(owner,"NOTE_CREATED","NOTE",n.getId(),"titleLength="+n.getTitle().length());return NoteResponse.from(n);
 }
 @Transactional(readOnly=true) public NoteResponse get(Long userId,Long id){return NoteResponse.from(owned(userId,id));}
 @Transactional public NoteResponse update(Long userId,Long id,NoteRequest r){
  Note n=owned(userId,id);n.update(r.title().trim(),r.content()==null?"":r.content(),clean(r.category()));
  audit.record(n.getUser(),"NOTE_UPDATED","NOTE",n.getId(),"titleLength="+n.getTitle().length());return NoteResponse.from(n);
 }
 @Transactional public void delete(Long userId,Long id){
  Note n=owned(userId,id);audit.record(n.getUser(),"NOTE_DELETED","NOTE",n.getId(),"contentRetained=false");notes.delete(n);
 }
 private Note owned(Long uid,Long id){return notes.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Note not found"));}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
 public record NoteRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=160) String title,
  @jakarta.validation.constraints.Size(max=50000) String content,@jakarta.validation.constraints.Size(max=60) String category){}
 public record NoteResponse(Long id,String title,String content,String category,Instant createdAt,Instant updatedAt){
  static NoteResponse from(Note n){return new NoteResponse(n.getId(),n.getTitle(),n.getContent(),n.getCategory(),n.getCreatedAt(),n.getUpdatedAt());}
 }
}