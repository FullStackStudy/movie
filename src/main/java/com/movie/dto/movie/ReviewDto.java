package com.movie.dto.movie;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class ReviewDto {

    private Long reviewId;
    private Long movieId;
    private String memberId;
    private String memberName;
    private String memberNickname;
    private Integer rating;
    private String content;
    private LocalDateTime regDate;
    private boolean canReview; // 리뷰 작성 가능 여부
} 