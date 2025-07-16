package com.movie.service.store;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

@Service
@Log
public class FileImgService {
    public String uploadFileImg(String uploadPath, String originalFileName,byte[] fileData)
            throws Exception{
        UUID uuid = UUID.randomUUID();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String savedFileName = uuid.toString()+extension;
        String fileUploadFulUrl = uploadPath+"/"+savedFileName;
        System.out.println(fileUploadFulUrl);
        FileOutputStream fos = new FileOutputStream(fileUploadFulUrl);
        fos.write(fileData);
        fos.close();
        return savedFileName;
    }
    public void deleterFileImg(String filePath)throws Exception{
        File deleteFile = new File(filePath);

        if (deleteFile.exists()){
            deleteFile.delete();
            log.info("파일을 삭제하였습니다.");
        }
        else {
            log.info("파일이 존재하지 않습니다.");
        }
    }
}
