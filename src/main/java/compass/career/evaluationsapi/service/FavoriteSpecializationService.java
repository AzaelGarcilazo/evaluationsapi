package compass.career.evaluationsapi.service;

import compass.career.evaluationsapi.dto.FavoriteSpecializationRequest;
import compass.career.evaluationsapi.dto.FavoriteSpecializationResponse;

import java.util.List;

public interface FavoriteSpecializationService {
    FavoriteSpecializationResponse addFavoriteSpecialization(Integer userId, FavoriteSpecializationRequest request);
    void removeFavoriteSpecialization(Integer userId, Integer specializationId);
    List<FavoriteSpecializationResponse> getFavoriteSpecializations(Integer userId, int page, int pageSize);
}