package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.payload.request.AlbumRequest;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @Author nanako
 * @Date 2025/8/14
 * @Description 相册控制器
 */

@RestController
@RequestMapping("/api/albums")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AlbumController {

    @Autowired
    private AlbumRepository albumRepository;

    //获取当前登录用户的所有相册
    @GetMapping
    public ResponseEntity<List<Album>> getAlbums() {

        //取出用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //获取用户id
       UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();

       //根据用户id从数据库获取相册
        List<Album> albums = albumRepository.findByUserId(userDetails.getId());
        return ResponseEntity.ok(albums);
    }

    //创建新相册
    @PostMapping
    public ResponseEntity<Album> createAlbum(@RequestBody AlbumRequest albumRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();
        Album album = new Album();
        album.setName(albumRequest.getName());
        album.setUserId(userDetails.getId());
        Album savedAlbum = albumRepository.save(album);
        return ResponseEntity.ok(savedAlbum);
    }

    //通过分享链接获取公开相册信息
    @GetMapping("/share/{shareToken}")
    public ResponseEntity<Album> getShareAlbum(@PathVariable String shareToken) {
        Optional<Album> albumData = albumRepository.findByShareToken(shareToken);
        return albumData.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
