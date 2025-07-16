    package com.movie.dto.store;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import lombok.Getter;
    import lombok.Setter;

    import java.time.LocalTime;

    @Getter
    @Setter
    public class CartDetailDto {
        private Long cartItemId;
        private String itemNm;
        private int price;
        private int count;
        private String imgUrl;
        private String itemComposition;
        private String cinemaName;
        private Boolean isPast;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime pickupTime;
        public CartDetailDto(long cartItemId,String itemNm,int price,int count,String imgUrl,LocalTime pickupTime,String itemComposition,Boolean isPast,String cinemaName){
            this.cartItemId = cartItemId;
            this.itemNm = itemNm;
            this.price = price;
            this.imgUrl = imgUrl;
            this.count = count;
            this.pickupTime = pickupTime;
            this.itemComposition = itemComposition;
            this.isPast = isPast;
            this.cinemaName = cinemaName;
        }
    }
