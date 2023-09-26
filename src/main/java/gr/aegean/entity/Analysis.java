package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Analysis {
    private Integer id;
    private Integer userId;
    private LocalDate createdDate;

    public Analysis(Integer userId, LocalDate createdDate) {
        this.userId = userId;
        this.createdDate = createdDate;
    }
}

