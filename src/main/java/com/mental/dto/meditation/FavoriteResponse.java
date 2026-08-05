package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteResponse {

    private boolean favorite;

    private String message;

}
