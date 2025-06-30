package com.movie.controller;

import com.movie.constant.Menu;
import com.movie.dto.ItemFormDto;
import com.movie.dto.ItemSearchDto;
import com.movie.entity.Item;
import com.movie.service.ItemService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codehaus.groovy.runtime.dgmimpl.arrays.ObjectArrayGetAtMetaMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    @GetMapping("/admin/item/new")
    public String itemForm(Model model){
        model.addAttribute("itemFormDto",new ItemFormDto());
        return "item/itemForm";
    }
    @PostMapping(value = "/admin/item/new")
    public String itemNew(@Valid ItemFormDto itemFormDto, BindingResult bindingResult, Model model,
                          @RequestParam("itemImgFile")List<MultipartFile> itemImgFileList, Menu menu){
        if (bindingResult.hasErrors()){
            return "item/itemForm";
        }
        if (itemImgFileList.get(0).isEmpty() && itemFormDto.getId() == null){
            model.addAttribute("errorMessage","첫 번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }
        try{
            itemService.saveItem(itemFormDto,itemImgFileList);
        }
        catch (Exception e){
            model.addAttribute("errorMessage","상품 등록 중 에러가 발생하였습니다.");
            return "item/itemForm";
        }
        if (itemFormDto.getMenu() == menu.COMBO){
            return "redirect:/store";
        } else if (itemFormDto.getMenu() == menu.POPCORN) {
            return "redirect:/popcorn";
        }
        else if (itemFormDto.getMenu() == menu.DRINK) {
            return "redirect:/drink";
        }
        else{
            return "redirect:/snack";
        }
    }
    @GetMapping(value = "/admin/item/{itemId}")
    public String itemDtl(@PathVariable("itemId")Long itemId,Model model){
        try{
            ItemFormDto itemFormDto = itemService.getItemDtl(itemId);
            model.addAttribute("itemFormDto",itemFormDto);
        }
        catch (EntityNotFoundException e){
            model.addAttribute("errorMessage","존재하지 않는 상품입니다.");
            model.addAttribute("itemFormDto",new ItemFormDto());
            return "item/itemForm";
        }
        return "item/itemForm";
    }
    @PostMapping(value = "/admin/item/{itemDto}")
    public String itemUpdate(@Valid ItemFormDto itemFormDto, BindingResult bindingResult,
                             @RequestParam("itemImgFile")List<MultipartFile> itemImgFileList,Model model){
        if (bindingResult.hasErrors()){
            return "item/itemForm";
        }
        if (itemImgFileList.get(0).isEmpty() && itemFormDto.getId() == null){
            model.addAttribute("errorMessage","첫 번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }
        try{
            itemService.updateItem(itemFormDto,itemImgFileList);
        }
        catch (Exception e){
            model.addAttribute("errorMessage","상품 수정 중 에러가 발생하였습니다.");
            return "item/itemForm";
        }
        return "redirect:/store";
    }
    @GetMapping(value = {"/admin/items","/admin/items/{page}"})
    public String itemManage(ItemSearchDto itemSearchDto, @PathVariable("page")Optional<Integer> page,
                             Model model){
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0,10);

        Page<Item> items = itemService.getAdminItemPage(itemSearchDto,pageable);
        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto",itemSearchDto);
        model.addAttribute("maxPage",10);
        return "item/itemMng";
    }

   @GetMapping(value = "/item/{itemId}")
    public String itemDtl(Model model,@PathVariable("itemId")Long itemId){
       ItemFormDto itemFormDto = itemService.getItemDtl(itemId);
       model.addAttribute("item",itemFormDto);
       return "item/itemDtl";
   }
}
