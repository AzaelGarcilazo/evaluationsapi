package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.SpecializationArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecializationAreaRepository extends JpaRepository<SpecializationArea, Integer> {
    List<SpecializationArea> findByCareerId(Integer careerId);
}