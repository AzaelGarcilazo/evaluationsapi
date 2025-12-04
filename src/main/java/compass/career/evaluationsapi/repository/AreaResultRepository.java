package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.AreaResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaResultRepository extends JpaRepository<AreaResult, Integer> {
    List<AreaResult> findByEvaluationIdOrderByRankingAsc(Integer evaluationId);
}