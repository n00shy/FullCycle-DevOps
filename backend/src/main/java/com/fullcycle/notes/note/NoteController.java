package com.fullcycle.notes.note;
import com.fullcycle.notes.user.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/notes")
public class NoteController {
 private final NoteService service;
 public NoteController(NoteService service){this.service=service;}
 @GetMapping public Page<NoteService.NoteResponse> list(Authentication auth,@RequestParam(required=false) String q,
  @RequestParam(required=false) String category,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  if(page<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page must be >= 0");
  if(size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"size must be 1..100");
  return service.list(user(auth).id(),q,category,page,size);
 }
 @PostMapping public NoteService.NoteResponse create(Authentication auth,@Valid @RequestBody NoteService.NoteRequest body){return service.create(user(auth).id(),body);}
 @GetMapping("/{id}") public NoteService.NoteResponse get(Authentication auth,@PathVariable Long id){return service.get(user(auth).id(),id);}
 @PatchMapping("/{id}") public NoteService.NoteResponse update(Authentication auth,@PathVariable Long id,@Valid @RequestBody NoteService.NoteRequest body){return service.update(user(auth).id(),id,body);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(Authentication auth,@PathVariable Long id){service.delete(user(auth).id(),id);return ResponseEntity.noContent().build();}
 private AppUserDetails user(Authentication auth){return (AppUserDetails)auth.getPrincipal();}
}