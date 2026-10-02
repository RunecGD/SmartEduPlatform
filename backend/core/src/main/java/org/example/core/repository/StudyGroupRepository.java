package org.example.core.repository;
import org.example.core.model.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    List<StudyGroup> findAllByOrderByNameAsc();
    List<StudyGroup> findByCurator_IdOrderByNameAsc(Long curatorId);
    boolean existsByCurator_IdAndStudents_Id(Long curatorId, Long studentId);
    boolean existsByCurator_Id(Long curatorId);
    boolean existsByStudents_Id(Long studentId);
    boolean existsByStudents_IdAndIdNot(Long studentId, Long groupId);
}
