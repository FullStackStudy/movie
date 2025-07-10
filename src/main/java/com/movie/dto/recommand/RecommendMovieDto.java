package com.movie.dto.recommand;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class RecommendMovieDto {
    private Long movieId;
    private String title;
    private String genres;
    private String actors;
    private String openDate;
   // 기본 정보 (연령, 상영시간, 개봉국가)
    private String detailInfo;

    private String rating;
    private int runtime;
    private String country;

    public void parseDetail(String detailInfo){
        this.detailInfo = detailInfo;
        if(detailInfo == null || detailInfo.isEmpty()){
            this.rating="";
            this.runtime=0;;
            this.country="";
            return;
        }

        String[] parts =detailInfo.split(",");
        this.rating = parts.length >0 ? parts[0].trim() : "";
        String runtimeStr = parts.length > 1 ? parts[1].trim() : "";
        this.country = parts.length > 2? parts[2].trim() : "";

        if(!runtimeStr.isEmpty()){
            try {
                this.runtime = Integer.parseInt(runtimeStr.replace("분", "").trim());

            }catch (NumberFormatException e){
                this.runtime=0;
            }
        }else{
            this.runtime=0;
        }

    }
}
