package com.mental.dto.dashboard;

import lombok.Builder;

@Builder
public record GrowthRateResponse(

        long currentMonthUsers,

        long previousMonthUsers,

        double growthPercentage

){}