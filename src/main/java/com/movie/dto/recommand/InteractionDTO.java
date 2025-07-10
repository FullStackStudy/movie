package com.movie.dto.recommand;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InteractionDTO {
    private String userId;
    private Long movieId;
}
