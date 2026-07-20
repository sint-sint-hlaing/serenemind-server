package com.mental.service.admin.impl;


import com.mental.dto.Notification.NotificationRequest;
import com.mental.dto.admin.NotificationDto;
import com.mental.model.entity.Notification;
import com.mental.model.entity.User;
import com.mental.repository.NotificationRepository;
import com.mental.repository.UserRepository;
import com.mental.service.admin.AdminNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl
        implements AdminNotificationService {


    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;



    @Override
    public NotificationDto createNotification(
            NotificationRequest request
    ) {


        Notification notification = new Notification();


        notification.setTitle(
                request.getTitle()
        );


        notification.setMessage(
                request.getMessage()
        );


        notification.setType(
                request.getType()
        );


        notification.setTargetType(
                request.getTarget()
        );


        /*
          If notification is for specific user
        */
        if(request.getUserId() != null){

            User user = userRepository.findById(
                            request.getUserId()
                    )
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "User not found"
                            )
                    );


            notification.setUser(user);

        }


        Notification saved =
                notificationRepository.save(notification);



        return NotificationDto.builder()

                .title(saved.getTitle())

                .message(saved.getMessage())

                .type(saved.getType())

                .target(saved.getTargetType())

                .userId(
                        saved.getUser() != null
                                ? saved.getUser().getId()
                                : null
                )

                .build();

    }

}