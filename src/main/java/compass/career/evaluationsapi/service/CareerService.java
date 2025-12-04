package compass.career.evaluationsapi.service;

import compass.career.evaluationsapi.dto.*;

import java.util.List;

public interface CareerService {
    List<CareerRecommendationResponse> getRecommendedCareers(Integer userId);
    CareerDetailResponse getCareerDetails(Integer careerId);
    List<CareerResponse> getAllCareers(int page, int pageSize);
    CareerResponse getCareerById(Integer careerId);
    CareerResponse createCareer(CareerRequest request);
    CareerResponse updateCareer(Integer careerId, CareerRequest request);
}