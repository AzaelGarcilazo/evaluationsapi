package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.Career;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Integer> {
}