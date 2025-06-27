package com.movie.controller;


import com.movie.dto.ItemDto;
import com.movie.dto.ItemFormDto;
import com.movie.dto.ItemSearchDto;
import com.movie.dto.StoreMainItemDto;
import com.movie.entity.Item;
import com.movie.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class StoreController {
   private final ItemService itemService;
   @GetMapping(value = "/store")
    public String StoreMain(ItemSearchDto itemSearchDto, Optional<Integer> page, Model model) {
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 10);
        Page<StoreMainItemDto> items = itemService.getStoreMainItemPage(itemSearchDto, pageable);
        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto",itemSearchDto);
        model.addAttribute("maxPage",10);
        return "store/store";
    }
    @GetMapping(value = "/popcorn")
    public String StorePopcorn(ItemSearchDto itemSearchDto, Optional<Integer> page, Model model) {
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 10);
        Page<StoreMainItemDto> items = itemService.getStoreMainItemPage(itemSearchDto, pageable);
        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto",itemSearchDto);
        model.addAttribute("maxPage",10);
        return "store/popcorn";
    }
    @GetMapping(value = "/drink")
    public String StoreDrink(ItemSearchDto itemSearchDto, Optional<Integer> page, Model model) {
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 10);
        Page<StoreMainItemDto> items = itemService.getStoreMainItemPage(itemSearchDto, pageable);
        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto",itemSearchDto);
        model.addAttribute("maxPage",10);
        return "store/drink";
    }
    @GetMapping(value = "/snack")
    public String StoreSnack(ItemSearchDto itemSearchDto, Optional<Integer> page, Model model) {
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 10);
        Page<StoreMainItemDto> items = itemService.getStoreMainItemPage(itemSearchDto, pageable);
        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto",itemSearchDto);
        model.addAttribute("maxPage",10);
        return "store/snack";
    }
}
