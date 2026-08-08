package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JournalPhotoResponse {

    private Long journalId;

    private String photoUrl;

    private String message;
}
