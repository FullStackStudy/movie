package com.movie.dto.recommand;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class TmdbDto { //flask에서 받은 데이터에 있는 movieid로 post title받아서 와꾸로 가져가는 용도 dto
    private Long movieId;
    private String moviePoster;
    private String movieTitle;
    private String releasedDate;
    private String detail;
    private String genres;
    private Double voteAverage;
}
