package com.mental.dto.goal;

import lombok.Data;
import java.util.List;

@Data
public class HomeResponse {
    private List<GoalResponse> goals;
    private Integer totalGoals;
    private Integer activeGoals;
    private Integer completedGoals;

}
