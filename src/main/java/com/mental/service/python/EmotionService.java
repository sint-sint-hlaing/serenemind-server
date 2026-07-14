package com.mental.service.python;

import com.mental.dto.emotional.EmotionRequest;
import com.mental.dto.emotional.EmotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class EmotionService {



    private final RestTemplate restTemplate;

    public EmotionResponse analyze(String text){

        EmotionRequest request =
                new EmotionRequest(text);

        return restTemplate.postForObject(

                "http://localhost:8000/analyze",

                request,

                EmotionResponse.class

        );

    }

}