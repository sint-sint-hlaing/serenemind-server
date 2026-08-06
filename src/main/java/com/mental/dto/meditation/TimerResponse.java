package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimerResponse {

    private Integer minutes;

    private String message;}
