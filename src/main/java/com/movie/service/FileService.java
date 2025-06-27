package com.movie.service;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload.path}")
    private String uploadPath;

    public String uploadFile(MultipartFile file) throws IOException {
        // 업로드 디렉토리가 없으면 생성
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 파일명 중복 방지를 위해 UUID 사용
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = Paths.get(uploadPath + savedFilename);
        Files.write(filePath, file.getBytes());

        // 웹에서 접근할 수 있는 경로 반환
        return "/uploads/" + savedFilename;
    }

    public void deleteFile(String filePath) {
        if (filePath != null && filePath.startsWith("/uploads/")) {
            String filename = filePath.substring("/uploads/".length());
            Path path = Paths.get(uploadPath + filename);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("파일 삭제 실패: " + e.getMessage());
            }
        }
    }
}