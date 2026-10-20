
package com.fullcycle.notes.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("""
        select n from Note n
        where n.user.id = :uid
          and (:category = '' or lower(n.category) = lower(:category))
          and (
              :q = ''
              or lower(n.title) like lower(concat('%', :q, '%'))
              or lower(n.content) like lower(concat('%', :q, '%'))
          )
        """)
    Page<Note> searchOwned(
        @Param("uid") Long userId,
        @Param("category") String category,
        @Param("q") String query,
        Pageable pageable
    );

    Optional<Note> findByIdAndUserId(Long id, Long userId);
}

