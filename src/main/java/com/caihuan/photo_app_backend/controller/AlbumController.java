package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Photo; // 【新增】导入
import com.caihuan.photo_app_backend.entity.User;
import com.caihuan.photo_app_backend.exception.ResourceNotFoundException;
import com.caihuan.photo_app_backend.payload.request.AlbumRequest;
import com.caihuan.photo_app_backend.payload.response.MessageResponse;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.repository.PhotoRepository; // 【新增】导入
import com.caihuan.photo_app_backend.repository.UserRepository;
import com.caihuan.photo_app_backend.services.AlbumService;
import com.caihuan.photo_app_backend.services.PhotoService;
import com.caihuan.photo_app_backend.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // 【新增】导入
import java.util.stream.Collectors; // 【新增】导入

@RestController
@RequestMapping("/api/albums")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AlbumController {

    private static final Logger logger = LoggerFactory.getLogger(AlbumController.class);

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository; // 【新增】注入 PhotoRepository

    @Autowired
    private PhotoService photoService;

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public ResponseEntity<List<Album>> getAlbums() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();
        List<Album> albums = albumRepository.findByUserId(userDetails.getId());
        return ResponseEntity.ok(albums);
    }

    @PostMapping
    public ResponseEntity<Album> createAlbum(@RequestBody AlbumRequest albumRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();

        Long userId = userDetails.getId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("错误: 未找到 ID 为 " + userId + " 的用户"));

        Album album = new Album();
        album.setName(albumRequest.getName());
        album.setUser(currentUser);

        Album savedAlbum = albumRepository.save(album);
        return ResponseEntity.ok(savedAlbum);
    }

    @PostMapping("/{albumId}/analyze")
    public ResponseEntity<?> startAlbumAnalysis(@PathVariable Long albumId) {
        photoService.analyzePhotosInAlbum(albumId);
        return ResponseEntity.accepted().body("相册分析任务已开始处理");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteAlbum(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();

        try {
            albumService.deleteAlbumAndAssociatedPhotos(id, userId);
            return ResponseEntity.ok(new MessageResponse("相册及其所有照片已成功删除！"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除相册 {} 时发生未知错误", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("删除相册时发生内部错误。"));
        }
    }

    @GetMapping("/{id}/share-link")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getShareLink(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();

        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到相册 " + id));

        if (!album.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("无权访问此相册"));
        }

        String shareToken = album.getShareToken();
        // 假设你的前端运行在 http://localhost:5173
        String shareUrl = "http://localhost:5173/album/share/" + shareToken;

        return ResponseEntity.ok(Map.of("shareLink", shareUrl));
    }

    @GetMapping("/{id}/liked-photos/export")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> exportLikedPhotoNames(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();

        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到相册 " + id));

        if (!album.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("无权访问此相册"));
        }

        List<String> likedPhotoNames = photoRepository.findByAlbumIdAndIsLikedByClientTrue(id)
                .stream()
                .map(Photo::getOriginalFileName)
                .collect(Collectors.toList());

        return ResponseEntity.ok(likedPhotoNames);
    }
}