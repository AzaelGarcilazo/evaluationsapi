package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query(value = "SELECT * FROM questions q WHERE q.test_id = :testId AND q.active = true ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<Question> findRandomActiveQuestionsByTestId(@Param("testId") Integer testId, @Param("limit") Integer limit);

    List<Question> findByTestIdAndActiveTrue(Integer testId);
}