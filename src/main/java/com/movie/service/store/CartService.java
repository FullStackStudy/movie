package com.movie.service.store;


import com.movie.dto.store.CartAddRequestDto;
import com.movie.dto.store.CartDetailDto;
import com.movie.entity.member.Member;
import com.movie.entity.store.Cart;
import com.movie.entity.store.CartItem;
import com.movie.entity.store.Item;
import com.movie.repository.cinema.CinemaRepository;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.store.CartItemRepository;
import com.movie.repository.store.CartRepository;
import com.movie.repository.store.ItemRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CinemaRepository cinemaRepository;

    public Long addCart(CartAddRequestDto cartItemDto, String memberId){
        Item item = itemRepository.findById(cartItemDto.getItemId())
                .orElseThrow(EntityExistsException::new);
        Member member = memberRepository.findByMemberId(memberId);
        Cart cart = cartRepository.findByMember_MemberId(member.getMemberId());

        if (cart == null){
            cart = Cart.createCart(member);
            cartRepository.save(cart);
        }


        // 🔍 기존 동일 항목 검색 시 영화관도 포함
        CartItem savedCartItem = cartItemRepository
                .findByCartIdAndItemIdAndPickupTimeAndCinemaName(cart.getId(), item.getId(), cartItemDto.getPickupTime(), cartItemDto.getCinemaName());

        if (savedCartItem != null){
            savedCartItem.addCount(cartItemDto.getCount());
            return savedCartItem.getId();
        } else {
            // 🔧 CartItem 생성 시 cinema 포함
            CartItem cartItem = CartItem.createCartItem(cart, item, cartItemDto.getCount(), cartItemDto.getPickupTime(),cartItemDto.getCinemaName());
            cartItemRepository.save(cartItem);
            return cartItem.getId();
        }
    }

    @Transactional(readOnly = true)
    public List<CartDetailDto> getCartList(String memberId){
        List<CartDetailDto> cartDetailDtoList = new ArrayList<>();
        Member member = memberRepository.findByMemberId(memberId);
        Cart cart = cartRepository.findByMember_MemberId(member.getMemberId());
        if (cart == null){
            return cartDetailDtoList;
        }
        cartDetailDtoList = cartItemRepository.findCartDetailDtoList(cart.getId());
        return cartDetailDtoList;
    }
    @Transactional(readOnly = true)
    public Boolean validateCartItem(Long cartItemId, String memberId){
        Member curMember = memberRepository.findByMemberId(memberId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityExistsException::new);
        Member savedMember = cartItem.getCart().getMember();
        if (!StringUtils.equals(curMember.getMemberId(),savedMember.getMemberId())){
            return false;
        }
        return true;
    }
    public void updateCartItemCount(Long cartItemId,int count){
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityExistsException::new);
        cartItem.updateCount(count);
    }
    public void deleteCartItem(Long cartItemId){
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityExistsException::new);
        cartItemRepository.delete(cartItem);
    }
    @Transactional
    public void updatePickupTime(String memberId) {
        Member member = memberRepository.findByMemberId(memberId);
        Cart cart = cartRepository.findByMember_MemberId(member.getMemberId());

        if (cart == null) {
            throw new IllegalStateException("장바구니가 없습니다.");
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니에 상품이 없습니다.");
        }
        LocalTime latestPickupTime = cartItems.get(cartItems.size()-1).getPickupTime();

        for (CartItem item : cartItems) {
            item.setPickupTime(latestPickupTime);
        }
    }
    }


