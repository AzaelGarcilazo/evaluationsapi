package compass.career.evaluationsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "area_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"evaluation_id", "vocational_area_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "evaluation_id", nullable = false)
    private CompletedEvaluation evaluation;

    @ManyToOne
    @JoinColumn(name = "vocational_area_id", nullable = false)
    private VocationalArea vocationalArea;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "ranking")
    private Integer ranking;
}