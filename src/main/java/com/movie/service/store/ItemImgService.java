package com.movie.service.store;


import com.movie.entity.store.ItemImg;
import com.movie.repository.store.ItemImgRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import java.awt.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemImgService {
    @Value("${itemImgLocation}")
    private String itemImgLocation;

    private final ItemImgRepository itemImgRepository;
    private final FileImgService fileImgService;

    public void saveItemImg(ItemImg itemImg, MultipartFile itemImgFile)throws Exception{
        String oriImgName = itemImgFile.getOriginalFilename();
        String imgName="";
        String imgUrl ="";

        if (!StringUtils.isEmpty(oriImgName)){
            imgName = fileImgService.uploadFileImg(itemImgLocation,oriImgName,
                    itemImgFile.getBytes());
            imgUrl="/images/item/"+imgName;
        }
        System.out.println("imguRl"+imgUrl);
        itemImg.updateItemImg(oriImgName,imgName,imgUrl);
        System.out.println("itemIMg"+itemImg);
        itemImgRepository.save(itemImg);
    }
    public void updateItemImg(Long itemImgId,MultipartFile itemImgFile)throws Exception{
        if (!itemImgFile.isEmpty()){
            ItemImg savedItemImg = itemImgRepository.findById(itemImgId).orElseThrow(EntityNotFoundException::new);
            if (!StringUtils.isEmpty(savedItemImg.getImgName())){
                fileImgService.deleterFileImg(itemImgLocation+"/"+savedItemImg.getImgName());
            }
            String oriImgName = itemImgFile.getOriginalFilename();
            String imgName = fileImgService.uploadFileImg(itemImgLocation,oriImgName,itemImgFile.getBytes());
            String imgUrl = "/images/item/"+imgName;
            savedItemImg.updateItemImg(oriImgName,imgName,imgUrl);
        }
    }
}
