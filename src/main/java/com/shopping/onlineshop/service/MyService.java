package com.shopping.onlineshop.service;

import com.shopping.onlineshop.model.Cart;
import com.shopping.onlineshop.model.Product;
import com.shopping.onlineshop.repository.MyRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class MyService {

    @Autowired
    private MyRepo repo ;

    @Autowired
    private RestTemplate restTemplate ;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor executor ;

    @Autowired
    private AsyncService asyncService ;
    
    @Autowired
    private Cart cart ; 

    @Async
    public CompletableFuture<String> transact() throws InterruptedException {
        return CompletableFuture.completedFuture("Your transaction initiated on thread " + Thread.currentThread().getName() + " id : " + UUID.randomUUID());
    }

    @Async
    public CompletableFuture<Product> saveProduct(Product product) {

        Long start = System.currentTimeMillis() ;

        log.info("Product {} saving began at thread : {} " , product , Thread.currentThread().getName() );
        Product saved = repo.save(product) ;
        Long end = System.currentTimeMillis() ;
        log.info("Product {} saved by thread {} time taken {} " , product , Thread.currentThread().getName() , end - start );

        return CompletableFuture.completedFuture(saved);
    }

    public List<Product> saveV2(List<Product> product) {
        return repo.saveAll(product);
    }

    public Boolean checkAvailability(Integer quantity, Long prodId) {
       return repo.findById(prodId).get().getQuantity() >= quantity;
    }

    public List<Cart> processOrder(Map<Long,Integer> details) {
        List<Cart> cart = new ArrayList<>();

        for(Map.Entry<Long,Integer> entry : details.entrySet()){
            Long id = entry.getKey();
            Integer quantity = entry.getValue();
            Map forObject = restTemplate.getForObject("http://localhost:8080/check/{quantity}/{id}", Map.class, quantity, id);
            assert forObject != null;
            if(forObject.get("IsAvailable").equals(Boolean.TRUE)){
                Optional<Product> p = repo.findById(id);
                Integer updatedQuantity = p.get().getQuantity() - quantity ;
                Double cost = p.get().getPrice() * quantity ; 
                
                 // add to cart 
                addToCart(p.get().getName() , quantity , cost , cart) ;

                p.get().setQuantity(updatedQuantity);
                
                repo.save(p.get()) ; 
            };

            Double totalAmount = generateBill(cart);

            processPayment(totalAmount) ;

            sendEmail() ;

            shareTrackingId() ;
        }

        return cart ;
    }

    private void processPayment(Double totalAmount) {
        try {
            Thread.sleep(1000); // process Payment
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Payment completed total Amount is {} " , totalAmount);
    }

    private void shareTrackingId() {
        Double trackingId = ( Math.random() * 300 ) ;
        log.info("Tracking id generated {} ", trackingId);
    }

    private Double generateBill(List<Cart> carts) {
        Double sum = (double) 0;
        for(Cart cart : carts){
            sum += cart.getProductCost() ;
        }

        log.info("Bill generated : Total = Rs {}" , sum );

        return sum ;
    }

    private void sendEmail() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String userId = UUID.randomUUID().toString().substring(0,5) ;
        log.info( "Email generated for User@{}gmail.com" , userId);
    }

    private void addToCart(String name, Integer quantity, Double cost , List<Cart> cart) {
           cart.add(new Cart(name,quantity,cost)) ; 
    }

    // section for consumer's handle order asynchronously

    public CompletableFuture<List<Cart>> placeOrder(Integer OrderId , Map<Long, Integer> details) {
            Integer id  = OrderId ;
            long startTime = System.currentTimeMillis() ;

        List<CompletableFuture<Cart>> allFutures = new ArrayList<>() ;

                List<Cart> finalCart = new ArrayList<>();

            // added to cart
            for(var entry : details.entrySet()){
                 CompletableFuture<Cart> future = asyncService.executeOrder(entry.getKey(), entry.getValue() );
                 allFutures.add(future) ;
            }

            // wait for each to complete
           return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
        // now all the futures inside allFuture executing async db entry work and making order cart completed
               .thenApply((voidOrderExec) -> {
                           for (CompletableFuture<Cart> eachCart : allFutures) {
                               finalCart.add(eachCart.join());
                           }
                          log.info("OrderId {} Your Cart is ready ! ! Please proceed to complete your order ", OrderId) ;
                           return finalCart;
                       })

                      .thenApply( (listCart) -> {
                          Double totalBill = asyncService.executeBill(OrderId, listCart);
                          return  totalBill ;
                       } )
                   .thenApply((amount) -> {

                        return asyncService.executePayment(OrderId , amount) ;
                   })
                   .thenApply((message)-> {
                       asyncService.sendEmail(OrderId) ;
                       asyncService.sendTracking(OrderId) ;
                       long endTime = System.currentTimeMillis();

                       log.info("OrderId : {} Task Completed  !  in millisec : {} ", id, endTime - startTime);
                       return finalCart ;
                   }) ;


    }


}
