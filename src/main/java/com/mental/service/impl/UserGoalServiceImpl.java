package com.mental.service.impl;

import com.mental.dto.GoalRequest;
import com.mental.dto.goal.UserGoal;
import com.mental.exception.UserNotFoundException;
import com.mental.model.entity.User;
import com.mental.model.entity.UserStreak;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.UserGoalRepository;
import com.mental.repository.UserRepository;
import com.mental.repository.UserStreakRepository;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserGoalServiceImpl implements UserGoalService {


    private final UserGoalRepository goalRepository;
    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;



    // Create Goal
    @Override
    public UserGoal createGoal(
            String email,
            GoalRequest request
    ){

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                ));



        com.mental.model.entity.UserGoal goal =
                new com.mental.model.entity.UserGoal();


        goal.setUser(user);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setTargetDays(request.targetDays());
        goal.setProgress(0);
        goal.setStatus(GoalStatus.ACTIVE);



        return mapToDto(
                goalRepository.save(goal)
        );
    }




    // Increase progress
    @Override
    public UserGoal updateProgress(
            Long id
    ){


        com.mental.model.entity.UserGoal goal =
                goalRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Goal not found"
                                ));



        UserStreak streak =
                streakRepository.findByUser(goal.getUser())
                        .orElseGet(() -> {

                            UserStreak newStreak =
                                    new UserStreak();

                            newStreak.setUser(
                                    goal.getUser()
                            );

                            newStreak.setStreakCount(0);

                            return streakRepository.save(
                                    newStreak
                            );
                        });



        LocalDate today =
                LocalDate.now();



        // only once per day
        if(streak.getLastCompleted() == null ||
                !streak.getLastCompleted()
                        .equals(today)){



            if(goal.getProgress()
                    < goal.getTargetDays()){


                goal.setProgress(
                        goal.getProgress()+1
                );

            }



            if(streak.getLastCompleted()!=null
                    &&
                    streak.getLastCompleted()
                            .equals(
                                    today.minusDays(1)
                            )){


                streak.setStreakCount(
                        streak.getStreakCount()+1
                );


            }else{


                streak.setStreakCount(1);

            }



            streak.setLastCompleted(today);


            streakRepository.save(streak);

        }




        if(goal.getProgress()
                >= goal.getTargetDays()){


            goal.setStatus(
                    GoalStatus.COMPLETED
            );

        }



        return mapToDto(
                goalRepository.save(goal)
        );

    }





    // Get Current User Goals
    @Override
    @Transactional(readOnly = true)
    public List<UserGoal> getUserGoals(
            String email
    ){


        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                ));



        return goalRepository.findByUser(user)
                .stream()
                .map(this::mapToDto)
                .toList();

    }





    // Complete Goal
    @Override
    public UserGoal completeGoal(
            Long id
    ){


        com.mental.model.entity.UserGoal goal =
                goalRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Goal not found"
                                ));



        goal.setProgress(
                goal.getTargetDays()
        );


        goal.setStatus(
                GoalStatus.COMPLETED
        );



        return mapToDto(
                goalRepository.save(goal)
        );

    }





    // Delete Goal
    @Override
    public void deleteGoal(
            Long id
    ){


        if(!goalRepository.existsById(id)){

            throw new RuntimeException(
                    "Goal not found"
            );

        }


        goalRepository.deleteById(id);

    }





    private UserGoal mapToDto(
            com.mental.model.entity.UserGoal goal
    ){


        return new UserGoal(

                goal.getId(),

                goal.getTitle(),

                goal.getDescription(),

                goal.getTargetDays(),

                goal.getProgress(),

                goal.getStatus()

        );

    }

}