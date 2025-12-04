package compass.career.evaluationsapi.mapper;

import compass.career.evaluationsapi.dto.CareerDetailResponse;
import compass.career.evaluationsapi.dto.CareerRecommendationResponse;
import compass.career.evaluationsapi.dto.FavoriteCareerRequest;
import compass.career.evaluationsapi.dto.FavoriteCareerResponse;
import compass.career.evaluationsapi.model.Career;
import compass.career.evaluationsapi.model.CareerRecommendation;
import compass.career.evaluationsapi.model.FavoriteCareer;

public final class CareerMapper {

    public static CareerRecommendationResponse toRecommendationResponse(CareerRecommendation entity) {
        if (entity == null)
            return null;

        Career career = entity.getCareer();
        return CareerRecommendationResponse.builder()
                .id(career.getId())
                .name(career.getName())
                .description(career.getDescription())
                .compatibilityPercentage(entity.getCompatibilityPercentage())
                .durationSemesters(career.getDurationSemesters())
                .averageSalary(career.getAverageSalary())
                .build();
    }

    public static CareerDetailResponse toDetailResponse(Career entity, Object socialMediaData) {
        if (entity == null)
            return null;

        return CareerDetailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .durationSemesters(entity.getDurationSemesters())
                .graduateProfile(entity.getGraduateProfile())
                .jobField(entity.getJobField())
                .averageSalary(entity.getAverageSalary())
                .socialMediaData(socialMediaData)
                .build();
    }

    public static FavoriteCareerResponse toFavoriteResponse(FavoriteCareer entity) {
        if (entity == null)
            return null;

        return FavoriteCareerResponse.builder()
                .id(entity.getId())
                .careerId(entity.getCareer().getId())
                .careerName(entity.getCareer().getName())
                .notes(entity.getNotes())
                .active(entity.getActive())
                .build();
    }

    public static FavoriteCareer toFavoriteEntity(FavoriteCareerRequest dto, Integer userId, Career career) {
        if (dto == null || userId == null || career == null)
            return null;

        FavoriteCareer entity = new FavoriteCareer();
        entity.setUserId(userId);
        entity.setCareer(career);
        entity.setNotes(dto.getNotes());
        entity.setActive(true);

        return entity;
    }
}