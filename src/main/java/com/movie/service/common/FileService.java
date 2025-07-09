package com.movie.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload.path}")
    private String uploadPath;

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 업로드 디렉토리가 없으면 생성
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("업로드 디렉토리를 생성할 수 없습니다: " + uploadPath);
            }
        }

        // 파일명 중복 방지를 위해 UUID 사용
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String savedFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = Paths.get(uploadPath + savedFilename);
        try {
        Files.write(filePath, file.getBytes());
            log.info("파일 업로드 성공: {}", savedFilename);
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            throw e;
        }

        // 웹에서 접근할 수 있는 경로 반환
        return "/uploads/" + savedFilename;
    }

    // upload 에 저장된 이미지 파일 삭제
    public void deleteFile(String filePath) {
        if (filePath != null && filePath.startsWith("/uploads/")) {
            String filename = filePath.substring("/uploads/".length());
            Path path = Paths.get(uploadPath + filename);
            try {
                boolean deleted = Files.deleteIfExists(path);
                if (deleted) {
                    log.info("파일 삭제 성공: {}", filename);
                } else {
                    log.warn("삭제할 파일이 존재하지 않음: {}", filename);
                }
            } catch (IOException e) {
                log.error("파일 삭제 실패: {}", e.getMessage(), e);
            }
        }
    }
} 