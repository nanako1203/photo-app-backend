package com.caihuan.photo_app_backend.payload.response;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Photo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SharedAlbumResponse {
    private Album album;
    private List<Photo> photos;
}
