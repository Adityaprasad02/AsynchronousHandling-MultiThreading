package com.shopping.onlineshop.controller;


import com.shopping.onlineshop.model.Cart;
import com.shopping.onlineshop.model.Product;
import com.shopping.onlineshop.service.MyService;
import com.shopping.onlineshop.model.OrderRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@Slf4j
public class MyController {

    @Autowired
    private MyService myService ;


    // for seller -> async saving
    @PostMapping("/seller")
    public  ResponseEntity<List<Product>> processSeller (@RequestBody List<Product> products) {

        for(Product product : products){
              myService.saveProduct(product) ;
        }

        return new ResponseEntity<>( products , HttpStatus.CREATED) ;

    }

    // for seller -> sync saving
    @PostMapping("/seller/v2")
    public ResponseEntity<List<Product>> process(@RequestBody List<Product> product){
        return new ResponseEntity<>(  myService.saveV2(product) , HttpStatus.CREATED) ;
    }

    @GetMapping("/check/{quantity}/{productId}")
    public ResponseEntity<Map<String,Boolean>> checkAvailability(@PathVariable("quantity") Integer quantity , @PathVariable("productId") Long prodId){
        Boolean res = myService.checkAvailability(quantity, prodId) ;
        return new ResponseEntity<>(Map.of("IsAvailable" , res ),HttpStatus.FOUND) ;
    }

    // for customer -> sync
    @GetMapping("/customer")
    public ResponseEntity<List<Cart>> processOrder (@RequestBody Map<Long,Integer> details) {
        List<Cart> carts = myService.processOrder(details);
        return ResponseEntity.status(HttpStatus.CREATED).body(carts) ;
    }

    // for customer -> async
    @PostMapping("/order")
    public ResponseEntity<List<Cart>> processAsync(@RequestBody Map<Long,Integer> details) {

        CompletableFuture<List<Cart>> carts = myService.placeOrder(101 , details);

        List<Cart> responseCart = carts.join();

        return  new ResponseEntity<>(responseCart,HttpStatus.CREATED) ;

    }

    @PostMapping("/multipleUsers")
    public ResponseEntity<List<List<Cart>>> multipleRequest(@RequestBody List<OrderRequestDto> request){
          // multiple request at a time

       List<CompletableFuture<List<Cart>>> response = new ArrayList<>() ;

        for (OrderRequestDto order : request) {
            CompletableFuture<List<Cart>> listCompletableFuture = myService.placeOrder(
                    order.getOrderId(),
                    order.getOrderRequest()
            );
            response.add(listCompletableFuture) ;
        }

        CompletableFuture.allOf(response.toArray(new CompletableFuture[0])).join() ;

        List<List<Cart>> cartsCollection = new ArrayList<>() ;

        for(CompletableFuture<List<Cart>> carts : response){
             cartsCollection.add(carts.join()) ;
        }

     return ResponseEntity.ok(cartsCollection);
    }

    @PostMapping("/sync/multiple")
    public ResponseEntity<List<List<Cart>>> syncMultipleRequest(@RequestBody List<OrderRequestDto> request){

        List<List<Cart>> resp = new ArrayList<>() ;
        for (OrderRequestDto order : request) {
            resp.add(myService.processOrder(
                    order.getOrderRequest()
            ));
        }
        return new ResponseEntity<>(resp ,HttpStatus.CREATED) ;
    }



}
