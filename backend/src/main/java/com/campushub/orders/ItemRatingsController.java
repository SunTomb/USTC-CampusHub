package com.campushub.orders;
import com.campushub.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
@RestController
public class ItemRatingsController {
    private final CatalogRatings ratings;
    public ItemRatingsController(CatalogRatings ratings){this.ratings=ratings;}
    @GetMapping("/api/service-items/{id}/ratings") public ApiResponse<CatalogRatings.Feedback> list(@PathVariable long id,@RequestParam(defaultValue="0") int page){return ApiResponse.ok(ratings.read(id,page));}
}
