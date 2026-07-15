package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "user_id",
                                "meditation_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {


    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="meditation_id")
    private Meditation meditation;


}
