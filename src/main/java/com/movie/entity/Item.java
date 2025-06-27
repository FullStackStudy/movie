package com.movie.entity;

import com.movie.constant.ItemSellStatus;
import com.movie.constant.Menu;
import com.movie.dto.ItemFormDto;
import com.movie.exception.OutOfStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "item")
@Getter
@Setter
@ToString
public class Item extends BaseEntity {
    @Id
    @Column(name = "item_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 50)
    private String itemNm;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(nullable = false)
    private int stockNumber;

    @Lob
    @Column(nullable = false)
    private String itemDetail;

    @Column(nullable = false)
    private String itemComposition;


    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus;

    @Enumerated(EnumType.STRING)
    private Menu menu;

    public void updateItem(ItemFormDto itemFormDto){
        this.id = itemFormDto.getId();
        this.itemNm = itemFormDto.getItemNm();
        this.price = itemFormDto.getPrice();
        this.stockNumber = itemFormDto.getStockNumber();
        this.itemDetail = itemFormDto.getItemDetail();
        this.itemComposition = itemFormDto.getItemComposition();
        this.itemSellStatus = itemFormDto.getItemSellStatus();
        this.menu = itemFormDto.getMenu();
    }

    public void removeStock(int stockNumber){
        int restStock = this.stockNumber - stockNumber;
        if (restStock < 0){
            throw new OutOfStockException("상품의 재고가 없습니다.");
        }
        this.stockNumber = restStock;
    }
    public void addStock(int stockNumber){this.stockNumber += stockNumber;}
}
