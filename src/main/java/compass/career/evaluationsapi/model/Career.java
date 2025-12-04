package compass.career.evaluationsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "careers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_semesters")
    private Integer durationSemesters;

    @Column(name = "graduate_profile", columnDefinition = "TEXT")
    private String graduateProfile;

    @Column(name = "job_field", columnDefinition = "TEXT")
    private String jobField;

    @Column(name = "average_salary", precision = 10, scale = 2)
    private BigDecimal averageSalary;

    @OneToMany(mappedBy = "career", cascade = CascadeType.ALL)
    private List<CareerRecommendation> careerRecommendations;

    @OneToMany(mappedBy = "career", cascade = CascadeType.ALL)
    private List<FavoriteCareer> favoriteCareers;

    @OneToMany(mappedBy = "career", cascade = CascadeType.ALL)
    private List<SpecializationArea> specializationAreas;
}