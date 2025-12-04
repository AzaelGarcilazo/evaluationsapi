package compass.career.evaluationsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "specialization_areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecializationArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "application_fields", columnDefinition = "TEXT")
    private String applicationFields;

    @Column(name = "job_projection", columnDefinition = "TEXT")
    private String jobProjection;

    @OneToMany(mappedBy = "specializationArea", cascade = CascadeType.ALL)
    private List<SpecializationRecommendation> specializationRecommendations;

    @OneToMany(mappedBy = "specializationArea", cascade = CascadeType.ALL)
    private List<FavoriteSpecialization> favoriteSpecializations;
}