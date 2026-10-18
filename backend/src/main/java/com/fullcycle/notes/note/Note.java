package com.fullcycle.notes.note;
import com.fullcycle.notes.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notes")
public class Note {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(nullable=false,length=160) private String title;
 @Column(nullable=false,columnDefinition="TEXT") private String content="";
 @Column(length=60) private String category;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected Note(){}
 public Note(AppUser user,String title,String content,String category){this.user=user;this.title=title;this.content=content;this.category=category;}
 @PreUpdate void beforeUpdate(){updatedAt=Instant.now();}
 public Long getId(){return id;} public AppUser getUser(){return user;} public String getTitle(){return title;} public String getContent(){return content;}
 public String getCategory(){return category;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
 public void update(String title,String content,String category){this.title=title;this.content=content;this.category=category;}
}