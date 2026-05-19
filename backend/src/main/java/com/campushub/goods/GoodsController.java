package com.campushub.goods;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsRepository goodsRepository;

    public GoodsController(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    @GetMapping
    public ApiResponse<List<GoodsSummary>> listGoods() {
        List<GoodsSummary> goods = goodsRepository.findByStatusOrderByCreatedAtDesc("ON_SALE").stream()
                .map(GoodsSummary::from)
                .toList();
        return ApiResponse.ok(goods);
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsSummary> getGoods(@PathVariable Long id) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new BusinessException("goods not found"));
        return ApiResponse.ok(GoodsSummary.from(goods));
    }
}
