package com.movie.repository.store;

import com.movie.entity.store.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart,Long> {
    Cart findByMember_MemberId(String memberId);

}
