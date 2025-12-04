package compass.career.evaluationsapi.service;

import compass.career.evaluationsapi.dto.*;

import java.util.List;

public interface SpecializationService {
    List<SpecializationRecommendationResponse> getRecommendedSpecializations(Integer userId);
    SpecializationDetailResponse getSpecializationDetails(Integer specializationId);
    List<SpecializationAreaResponse> getAllSpecializations();
    SpecializationAreaResponse getSpecializationById(Integer specializationId);
    SpecializationAreaResponse createSpecialization(SpecializationAreaRequest request);
    SpecializationAreaResponse updateSpecialization(Integer specializationId, SpecializationAreaRequest request);
}