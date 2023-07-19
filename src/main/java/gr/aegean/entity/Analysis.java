package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Analysis {
    private Integer id;
    private Integer userId;
    private LocalDateTime createdDate;

    public Analysis(Integer userId, LocalDateTime createdDate) {
        this.userId = userId;
        this.createdDate = createdDate;
    }
}

