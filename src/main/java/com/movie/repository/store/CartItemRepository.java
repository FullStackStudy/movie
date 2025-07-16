package com.movie.repository.store;


import com.movie.dto.store.CartDetailDto;
import com.movie.entity.store.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {
    CartItem findByCartIdAndItemIdAndPickupTime(Long carId, Long itemId, LocalTime pickupTime);

    @Query("select new com.movie.dto.store.CartDetailDto(ci.id, i.itemNm, i.price, ci.count, im.imgUrl, ci.pickupTime,i.itemComposition,ci.isPast,ci.cinemaName" +
            ") " +
            "from CartItem ci, ItemImg im "+
            "join ci.item i " +
            "where ci.cart.id = :cartId " +
            "and im.item.id = ci.item.id " +
            "and im.repImgYn = 'Y' " +
            "order by ci.regTime desc")
    List<CartDetailDto> findCartDetailDtoList(Long cartId);
    List<CartItem> findByCartId(Long cartId);
    List<CartItem> findByCart_Member_MemberIdAndIsCartTrue(String memberId);

    List<CartItem> findByIdIn(List<Long> cartItemIds);
    CartItem findByCartIdAndItemIdAndPickupTimeAndCinemaName(Long carId, Long itemId, LocalTime pickupTime,String cinemaName);
    void deleteByItem_ItemNmAndItem_ItemCompositionAndCinemaNameAndCart_Member_MemberIdAndIsCartTrue(
            String itemNm,
            String itemComposition,
            String cinemaName,
            String memberId
    );


}
