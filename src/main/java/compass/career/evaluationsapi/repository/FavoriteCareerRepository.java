package compass.career.evaluationsapi.repository;

import compass.career.evaluationsapi.model.FavoriteCareer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteCareerRepository extends JpaRepository<FavoriteCareer, Integer> {
    List<FavoriteCareer> findByUserIdAndActiveTrue(Integer userId, Pageable pageable);
    Optional<FavoriteCareer> findByUserIdAndCareerId(Integer userId, Integer careerId);
    long countByUserIdAndActiveTrue(Integer userId);
}