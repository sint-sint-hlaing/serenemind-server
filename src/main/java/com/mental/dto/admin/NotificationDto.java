package com.mental.dto.admin;


import lombok.Builder;


@Builder
public record NotificationDto(

        String title,

        String message,

        String type,

        String target,

        Long userId

) {

}