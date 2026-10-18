package com.fullcycle.notes.note;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface NoteRepository extends JpaRepository<Note,Long>{
 @Query("select n from Note n where n.user.id=:uid and (:category is null or lower(n.category)=lower(:category)) and (:q is null or lower(n.title) like lower(concat('%',:q,'%')) or lower(n.content) like lower(concat('%',:q,'%')))")
 Page<Note> searchOwned(@Param("uid") Long userId,@Param("category") String category,@Param("q") String query,Pageable pageable);
 Optional<Note> findByIdAndUserId(Long id,Long userId);
}