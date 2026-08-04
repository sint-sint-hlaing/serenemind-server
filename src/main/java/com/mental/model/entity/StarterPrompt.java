package com.mental.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "starter_prompts")
@Getter
@Setter
public class StarterPrompt extends BaseEntity {

    private String title;       // e.g. "I'm feeling stressed"
    private String subtitle;    // e.g. "Talk about what's on your mind"
    private String iconName;    // e.g. "ic_stressed_emoji"
    private String promptText;  // e.g. "I'm feeling stressed and overwhelmed with my tasks."
}