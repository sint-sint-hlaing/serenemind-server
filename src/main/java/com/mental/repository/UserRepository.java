package com.mental.repository;

import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<User,Long>{

    Optional<User> findByEmail(String email);


    long countByLastLoginAfter(LocalDateTime localDateTime);
    long countByLoginTimeAfter(LocalDateTime dateTime);

    Optional<User> findByUsername(String username);

}
