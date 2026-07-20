package com.mental.repository;

import com.mental.model.entity.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    List<Avatar> findByIsActiveTrue();
}
