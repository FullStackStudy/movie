package com.movie.repository;

import com.movie.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    
    Optional<Member> findByNickname(String nickname);
}