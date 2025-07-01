package com.movie.service;

import com.movie.dto.ReviewDto;
import com.movie.entity.Member;
import com.movie.entity.Movie;
import com.movie.entity.Review;
import com.movie.constant.Role;
import com.movie.repository.MemberRepository;
import com.movie.repository.MovieRepository;
import com.movie.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final MemberRepository memberRepository;

    // 영화별 리뷰 목록 조회 (페이지네이션)
    public Page<ReviewDto> getReviewsByMovieIdPaged(Long movieId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByMovieMovieIdOrderByRegDateDesc(movieId, pageable);
        return reviewPage.map(this::convertToDto);
    }
    
    // 영화별 리뷰 목록 조회 (전체)
    public List<ReviewDto> getReviewsByMovieId(Long movieId) {
        List<Review> reviews = reviewRepository.findByMovieMovieIdOrderByRegDateDesc(movieId);
        return reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 회원이 특정 영화에 리뷰 작성 가능한지 확인
    public boolean canWriteReview(Long movieId, String memberId) {
        log.info("리뷰 작성 권한 확인 시작: movieId={}, memberId={}", movieId, memberId);
        
        // 1. 회원이 존재하는지 확인
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.warn("회원을 찾을 수 없습니다: memberId={}", memberId);
            return false;
        }
        log.info("회원 정보 확인: name={}, role={}", member.getName(), member.getRole());

        // 2. 영화가 존재하는지 확인
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) {
            log.warn("영화를 찾을 수 없습니다: movieId={}", movieId);
            return false;
        }
        log.info("영화 정보 확인: title={}", movie.getMovieTitle());

        // 3. 관리자인 경우 무제한 리뷰 작성 가능
        if (member.getRole() == Role.ADMIN) {
            log.info("관리자 권한 확인됨: {}", member.getRole());
            log.info("관리자는 무제한 리뷰 작성 가능");
            return true;
        }

        // 4. 일반 사용자의 경우 예매내역 확인
        String memberReserve = member.getReserve();
        if (memberReserve == null || memberReserve.isEmpty()) {
            log.warn("예매내역이 없습니다: memberId={}", memberId);
            return false;
        }

        // 예매내역에 영화 제목이 포함되어 있는지 확인
        boolean hasReserved = memberReserve.contains(movie.getMovieTitle());
        log.info("예매내역 확인: reserve={}, hasReserved={}", memberReserve, hasReserved);
        
        // 5. 이미 리뷰를 작성했는지 확인
        boolean alreadyReviewed = reviewRepository.existsByMovieMovieIdAndMemberMemberId(movieId, memberId);
        log.info("이미 리뷰 작성 여부: {}", alreadyReviewed);

        boolean result = hasReserved && !alreadyReviewed;
        log.info("최종 리뷰 작성 권한: {}", result);
        return result;
    }

    // 리뷰 작성
    public ReviewDto writeReview(Long movieId, String memberId, Integer rating, String content) {
        log.info("리뷰 작성 시작: movieId={}, memberId={}, rating={}", movieId, memberId, rating);
        
        // 리뷰 작성 권한 확인
        boolean canReview = canWriteReview(movieId, memberId);
        log.info("리뷰 작성 권한 확인 결과: {}", canReview);
        
        if (!canReview) {
            log.error("리뷰 작성 권한이 없습니다: movieId={}, memberId={}", movieId, memberId);
            throw new IllegalArgumentException("리뷰를 작성할 수 없습니다. 영화를 예매했는지 확인해주세요.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
        log.info("영화 정보 조회 완료: {}", movie.getMovieTitle());
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        log.info("회원 정보 조회 완료: {} ({})", member.getName(), member.getRole());

        Review review = new Review();
        review.setMovie(movie);
        review.setMember(member);
        review.setRating(rating);
        review.setContent(content);

        try {
            Review savedReview = reviewRepository.save(review);
            log.info("리뷰 저장 완료: reviewId={}, 영화={}, 회원={}, 평점={}", 
                    savedReview.getReviewId(), movie.getMovieTitle(), member.getName(), rating);
            return convertToDto(savedReview);
        } catch (Exception e) {
            log.error("리뷰 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("리뷰 저장에 실패했습니다.", e);
        }
    }

    // 리뷰 수정
    public ReviewDto updateReview(Long reviewId, String memberId, Integer rating, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 관리자이거나 본인이 작성한 리뷰인지 확인
        boolean isAdmin = member.getRole() == Role.ADMIN;
        boolean isOwner = review.getMember().getMemberId().equals(memberId);
        
        if (!isAdmin && !isOwner) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.setRating(rating);
        review.setContent(content);

        Review updatedReview = reviewRepository.save(review);
        log.info("리뷰 수정 완료: 리뷰ID={}, 회원={}, 관리자여부={}", reviewId, memberId, isAdmin);

        return convertToDto(updatedReview);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, String memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 관리자이거나 본인이 작성한 리뷰인지 확인
        boolean isAdmin = member.getRole() == Role.ADMIN;
        boolean isOwner = review.getMember().getMemberId().equals(memberId);
        
        if (!isAdmin && !isOwner) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료: 리뷰ID={}, 회원={}, 관리자여부={}", reviewId, memberId, isAdmin);
    }

    // 회원이 관리자인지 확인
    public boolean isAdmin(String memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        return member != null && member.getRole() == Role.ADMIN;
    }

    // 영화별 평균 평점 조회
    public Double getAverageRatingByMovieId(Long movieId) {
        return reviewRepository.getAverageRatingByMovieId(movieId);
    }

    // 영화별 리뷰 개수 조회
    public Long getReviewCountByMovieId(Long movieId) {
        return reviewRepository.getReviewCountByMovieId(movieId);
    }

    // Entity → DTO 변환
    private ReviewDto convertToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setMovieId(review.getMovie().getMovieId());
        dto.setMemberId(review.getMember().getMemberId());
        dto.setMemberName(review.getMember().getName());
        dto.setMemberNickname(review.getMember().getNickname());
        dto.setRating(review.getRating());
        dto.setContent(review.getContent());
        dto.setRegDate(review.getRegDate());
        dto.setCanReview(false); // 이미 작성한 리뷰이므로 false
        return dto;
    }
} 