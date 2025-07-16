package com.movie.repository.member;

import com.movie.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    
    Optional<Member> findByNickname(String nickname);
    Member findByMemberId(String memberId);
} 