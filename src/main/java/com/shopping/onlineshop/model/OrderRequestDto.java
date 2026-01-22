package com.shopping.onlineshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {

    private Integer OrderId ;
    private Map<Long,Integer> orderRequest ;

}
