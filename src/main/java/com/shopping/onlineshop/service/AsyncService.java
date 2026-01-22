package com.shopping.onlineshop.service;


import com.shopping.onlineshop.model.Cart;
import com.shopping.onlineshop.model.Product;
import com.shopping.onlineshop.repository.MyRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AsyncService {

    @Autowired
    private RestTemplate restTemplate ;

    @Autowired
    private MyRepo repo ;

    @Async
    public CompletableFuture<Cart> executeOrder(Long key, Integer value) {
        Cart cartProduct = new Cart() ;
        Map forObject = restTemplate.getForObject("http://localhost:8080/check/{quantity}/{id}", Map.class, value, key);
        assert forObject != null;
        if(Boolean.TRUE.equals(forObject.get("IsAvailable"))){
            Optional<Product> p = repo.findById(key) ;
            Double totalCost = p.get().getPrice() * value ;
            Integer updatedStock = p.get().getQuantity() - value ;
            repo.save(p.get()) ;
            cartProduct.setProductName(p.get().getName());
            cartProduct.setProductCost(totalCost);
            cartProduct.setProductQuantity(value);
        }
        log.info("added Product id : {} with thread {} " ,  key , Thread.currentThread().getName());

        // returns a CompletedFuture<Cart>

        return CompletableFuture.completedFuture(cartProduct) ;

    }


     public Double executeBill(Integer orderId, List<Cart> cartCollection){
        Double totalSum = 0.00 ;
        for(Cart cart : cartCollection){
           totalSum += cart.getProductCost() ;
        }
        log.info("Order : {} Bill generated successfully ! Please Pay Rs . {} " ,orderId , totalSum );

        return (totalSum) ;
     }


     public String executePayment(Integer orderId, Double totalCost){

         try {
             Thread.sleep(1000);
         } catch (InterruptedException e) {
             throw new RuntimeException(e);
         }

         log.info("OrderId : {} Payment completed Rs.{} by thread {} " , orderId, totalCost , Thread.currentThread().getName());

         return "Payment Processed" ;

     }

    @Async("asyncTaskExecutor")
    public CompletableFuture<Void> sendEmail(Integer orderId){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("OrderId : {} Email sent to user@{}.com by thread {} " , orderId , UUID.randomUUID().toString().toUpperCase().substring(0,5) , Thread.currentThread().getName()) ;
        return CompletableFuture.completedFuture(null);
    }

    @Async("asyncTaskExecutor")
    public CompletableFuture<Void> sendTracking(Integer orderId){
        double trackingId = (Math.random() * 300.00) / 300.00;
        log.info("OrderId : {} Tracking id generated {} by thread {} " , orderId ,trackingId , Thread.currentThread().getName());
        return CompletableFuture.completedFuture(null);
    }
}
