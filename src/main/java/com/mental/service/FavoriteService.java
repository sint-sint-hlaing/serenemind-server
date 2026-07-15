package com.mental.service;

import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Favorite;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.User;
import com.mental.repository.FavoriteRepository;
import com.mental.repository.MeditationRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {


    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final MeditationRepository meditationRepository;



    public void addFavorite(
            Long userId,
            Long meditationId
    ){


        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );



        Meditation meditation =
                meditationRepository.findById(meditationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Meditation not found"
                                )
                        );



        boolean exists =
                favoriteRepository
                        .existsByUserIdAndMeditationId(
                                userId,
                                meditationId
                        );



        if(exists){

            throw new RuntimeException(
                    "Meditation already added to favorite"
            );
        }



        Favorite favorite =
                Favorite.builder()
                        .user(user)
                        .meditation(meditation)
                        .build();



        favoriteRepository.save(favorite);

    }

}