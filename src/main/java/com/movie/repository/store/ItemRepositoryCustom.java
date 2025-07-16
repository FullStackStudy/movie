package com.movie.repository.store;

import com.movie.dto.store.ItemSearchDto;
import com.movie.dto.store.StoreMainItemDto;

import com.movie.entity.store.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable);
    Page<StoreMainItemDto> getStoreMainItemPage(ItemSearchDto itemSearchDto,Pageable pageable);
}
