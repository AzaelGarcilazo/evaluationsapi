package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.SpecializationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpecializationRecommendationRepository extends JpaRepository<SpecializationRecommendation, Integer> {

    @Query("SELECT sr FROM SpecializationRecommendation sr JOIN FETCH sr.specializationArea sa JOIN FETCH sa.career WHERE sr.userId = :userId ORDER BY sr.compatibilityPercentage DESC")
    List<SpecializationRecommendation> findByUserIdOrderByCompatibilityPercentageDesc(@Param("userId") Integer userId);

    void deleteByUserId(Integer userId);
}