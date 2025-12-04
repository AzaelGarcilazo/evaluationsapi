package compass.career.evaluationsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "completed_evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletedEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL)
    private List<UserAnswer> userAnswers;

    @OneToOne(mappedBy = "evaluation", cascade = CascadeType.ALL)
    private EvaluationResult evaluationResult;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL)
    private List<AreaResult> areaResults;
}