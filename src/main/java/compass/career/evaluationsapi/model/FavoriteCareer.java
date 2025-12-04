package compass.career.evaluationsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorite_careers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "career_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCareer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active")
    private Boolean active = true;
}