package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Integer> {
    List<UserAnswer> findByEvaluationIdOrderByQuestionId(Integer evaluationId);
}