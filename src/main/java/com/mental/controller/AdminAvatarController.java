package com.mental.controller;

import com.mental.dto.AvatarResponse;
import com.mental.dto.MessageResponse;
import com.mental.service.AdminAvatarService;
import com.mental.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/admin/avatars")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAvatarController {


    private final AdminAvatarService adminAvatarService;
    private final AvatarService avatarService;



    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarResponse createAvatar(

            @RequestParam String name,

            @RequestPart MultipartFile image

    ){

        return adminAvatarService.createAvatar(
                name,
                image
        );
    }



    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AvatarResponse updateAvatar(

            @PathVariable Long id,

            @RequestParam(required = false)
            String name,

            @RequestPart(required = false)
            MultipartFile image

    ){

        return adminAvatarService.updateAvatar(
                id,
                name,
                image
        );
    }



    @DeleteMapping("/{id}")
    public MessageResponse deleteAvatar(

            @PathVariable Long id

    ){

        return adminAvatarService.deleteAvatar(id);
    }



    @GetMapping
    public List<AvatarResponse> getAllAvatarsForAdmin(){

        return avatarService.getAllAvatars();
    }
}