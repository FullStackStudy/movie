package com.movie.controller.movie;

import com.movie.dto.movie.ReviewDto;
import com.movie.service.movie.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 폼
    @GetMapping("/review/write/{movieId}")
    public String reviewWriteForm(@PathVariable Long movieId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = auth.getName();
        
        boolean canReview = reviewService.canWriteReview(movieId, memberId);
        
        model.addAttribute("movieId", movieId);
        model.addAttribute("canReview", canReview);
        
        return "review/writeForm";
    }

    // 리뷰 작성 처리
    @PostMapping("/review/write")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> writeReview(@RequestParam Long movieId,
                                                          @RequestParam Integer rating,
                                                          @RequestParam String content) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String memberId = auth.getName();
            
            log.info("리뷰 작성 요청: movieId={}, memberId={}, rating={}", movieId, memberId, rating);
            
            ReviewDto review = reviewService.writeReview(movieId, memberId, rating, content);
            
            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 작성되었습니다.");
            response.put("reviewId", review.getReviewId());
            response.put("movieId", review.getMovieId());
            response.put("memberId", review.getMemberId());
            response.put("rating", review.getRating());
            response.put("content", review.getContent());
            
            log.info("리뷰 작성 성공: reviewId={}", review.getReviewId());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(response);
        } catch (Exception e) {
            log.error("리뷰 작성 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(response);
        }
    }

    // 리뷰 수정 처리
    @PutMapping("/review/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateReview(@PathVariable Long reviewId,
                                                           @RequestParam Integer rating,
                                                           @RequestParam String content) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String memberId = auth.getName();
            
            ReviewDto review = reviewService.updateReview(reviewId, memberId, rating, content);
            
            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 수정되었습니다.");
            response.put("reviewId", review.getReviewId());
            response.put("rating", review.getRating());
            response.put("content", review.getContent());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(response);
        }
    }

    // 리뷰 삭제 처리
    @DeleteMapping("/review/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String memberId = auth.getName();
            
            reviewService.deleteReview(reviewId, memberId);
            
            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(response);
        }
    }

    // 리뷰 목록 조회 (AJAX)
    @GetMapping("/review/list/{movieId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReviews(@PathVariable Long movieId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ReviewDto> reviews = reviewService.getReviewsByMovieId(movieId);
            Double averageRating = reviewService.getAverageRatingByMovieId(movieId);
            Long reviewCount = reviewService.getReviewCountByMovieId(movieId);
            
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("averageRating", averageRating != null ? averageRating : 0.0);
            response.put("reviewCount", reviewCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 리뷰 작성 가능 여부 확인 (AJAX)
    @GetMapping("/review/can-write/{movieId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> canWriteReview(@PathVariable Long movieId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String memberId = auth.getName();
            
            boolean canReview = reviewService.canWriteReview(movieId, memberId);
            
            response.put("success", true);
            response.put("canReview", canReview);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 