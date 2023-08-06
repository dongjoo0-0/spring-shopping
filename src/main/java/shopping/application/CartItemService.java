package shopping.application;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shopping.domain.cart.CartItem;
import shopping.domain.cart.Product;
import shopping.dto.request.CartItemCreateRequest;
import shopping.dto.request.CartItemUpdateRequest;
import shopping.dto.response.CartItemResponse;
import shopping.exception.CartItemNotFoundException;
import shopping.exception.ProductNotFoundException;
import shopping.exception.UserNotMatchException;
import shopping.repository.CartItemRepository;
import shopping.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@Service
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartItemService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartItemResponse createCartItem(Long userId, CartItemCreateRequest cartItemCreateRequest) {
        Product product = productRepository
                .findById(cartItemCreateRequest.getProductId())
                .orElseThrow(ProductNotFoundException::new);

        Optional<CartItem> originalCartItem = cartItemRepository
                .findByUserIdAndProduct(userId, product);

        return CartItemResponse.of(
                cartItemRepository.save(
                        originalCartItem.map(CartItem::addQuantity)
                                .orElse(new CartItem(userId, product))));
    }

    public List<CartItemResponse> findAllByUserId(Long userId, Pageable pageable) {
        return cartItemRepository.findAllByUserId(userId, pageable)
                .map(CartItemResponse::of)
                .getContent();
    }

    @Transactional
    public CartItemResponse updateCartItemQuantity(Long userId, Long cartItemId, CartItemUpdateRequest cartItemUpdateRequest) {
        CartItem cartItem = findCartItem(userId, cartItemId);
        CartItem updatedCartItem = cartItem.updateQuantity(cartItemUpdateRequest.getQuantity());
        return CartItemResponse.of(cartItemRepository.save(updatedCartItem));
    }

    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {
        findCartItem(userId, cartItemId);
        cartItemRepository.deleteById(cartItemId);
    }

    private CartItem findCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(String.valueOf(cartItemId)));

        validateUserMatch(userId, cartItem);
        return cartItem;
    }

    private void validateUserMatch(Long userId, CartItem cartItem) {
        if (cartItem.isNotUserMatch(userId)) {
            throw new UserNotMatchException();
        }
    }
}