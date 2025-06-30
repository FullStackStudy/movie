package com.movie.repository;

import com.movie.dto.ItemSearchDto;
import com.movie.dto.StoreMainItemDto;
import com.movie.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable);
    Page<StoreMainItemDto> getStoreMainItemPage(ItemSearchDto itemSearchDto,Pageable pageable);
}
