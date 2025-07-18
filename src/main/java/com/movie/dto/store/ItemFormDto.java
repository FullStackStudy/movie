package com.movie.dto.store;


import com.movie.constant.ItemSellStatus;
import com.movie.constant.Menu;
import com.movie.entity.store.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ItemFormDto {
    private Long id;

    @NotBlank(message = "상품명은 필수 입력 값입니다.")
    private String itemNm;

    @NotNull(message = "가격은 필수 입력 값입니다.")
    private Integer price;

    @NotBlank(message = "상세는 필수 입력 값입니다.")
    private String itemDetail;

    @NotBlank(message = "구성은 필수 입력 값입니다.")
    private String itemComposition;

    @NotNull(message = "재고는 필수 입력 값입니다.")
    private Integer stockNumber;

    private ItemSellStatus itemSellStatus;

    private Menu menu;

    private List<ItemImgDto> itemImgDtoList = new ArrayList<>();

    private List<Long> itemImgIds = new ArrayList<>();

//    private LocalTime moveTime;

    private static ModelMapper modelMapper = new ModelMapper();
    public Item createItem(){
        return modelMapper.map(this,Item.class);
    }
    public static ItemFormDto of(Item item){
        return modelMapper.map(item,ItemFormDto.class);
    }
}
