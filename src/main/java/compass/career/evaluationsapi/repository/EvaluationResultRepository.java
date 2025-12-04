package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Integer> {
    Optional<EvaluationResult> findByEvaluationId(Integer evaluationId);
}