package com.shopping.onlineshop.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    String productName ;
    Integer productQuantity ;
    Double productCost ;

}
