package com.luv2code.ecommerce.service;

import com.luv2code.ecommerce.entity.Cart;
import com.luv2code.ecommerce.entity.CartItem;
import com.luv2code.ecommerce.entity.Product;
import com.luv2code.ecommerce.entity.User;
import com.luv2code.ecommerce.dao.CartRepository;
import com.luv2code.ecommerce.dao.CartItemRepository;
import com.luv2code.ecommerce.dao.ProductRepository;
import com.luv2code.ecommerce.dao.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class CartService {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_CART_ITEMS = 1000;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
    }

    public CartItem addProductToCart(Long userId, Long productId, int quantity) {
        Cart cart = getCartByUserId(userId);
        Product prod = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (quantity < MIN_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be at least " + MIN_QUANTITY);
        }

        if (prod.getUnitsInStock() < quantity) {
            throw new RuntimeException("Out of stock");
        }

        CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        CartItem item;
        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            if (prod.getUnitsInStock() < newQty) {
                throw new RuntimeException("Qty exceeds stock");
            }
            existing.setQuantity(newQty);
            item = cartItemRepository.save(existing);
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(prod);
            item.setQuantity(quantity);
            item.setUnitPrice(prod.getUnitPrice());
            item = cartItemRepository.save(item);
            cart.getCartItems().add(item);
        }
        updateCartTotals(cart);
        return item;
    }

    public void removeProductFromCart(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to this cart");
        }

        // Remove from collection and delete
        cart.getCartItems().remove(cartItem);
        cartItemRepository.deleteById(cartItemId);
    }

    public CartItem updateCartItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getCartByUserId(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Wrong cart");
        }

        if (quantity < MIN_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be at least " + MIN_QUANTITY);
        }

        Product p = item.getProduct();
        if (p.getUnitsInStock() < quantity) {
            throw new RuntimeException("Stock unavailable");
        }

        item.setQuantity(quantity);
        updateCartTotals(cart);
        return cartItemRepository.save(item);
    }

    public Cart viewCart(Long userId) {
        return getCartByUserId(userId);
    }

    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalQuantity(0);
        cartRepository.save(cart);
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setCartItems(new HashSet<>());
                    return cartRepository.save(cart);
                });
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalQuantity = 0;

        if (cart.getCartItems() != null) {
            for (CartItem item : cart.getCartItems()) {
                BigDecimal itemTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                totalPrice = totalPrice.add(itemTotal);
                totalQuantity += item.getQuantity();
            }
        }

        cart.setTotalPrice(totalPrice);
        cart.setTotalQuantity(totalQuantity);
        cartRepository.save(cart);
    }
}
