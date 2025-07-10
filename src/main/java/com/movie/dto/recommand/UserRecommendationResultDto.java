package com.movie.dto.recommand;

import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserRecommendationResultDto { //flask서버에서 온 결과 받는 dto
    private Long movieId;    // 추천된 영화 ID
    private double score;
}
