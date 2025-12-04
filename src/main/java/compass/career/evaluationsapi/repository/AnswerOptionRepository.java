package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Integer> {
}