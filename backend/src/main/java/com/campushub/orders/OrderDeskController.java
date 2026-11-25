package com.campushub.orders;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders")
public class OrderDeskController {
    record Rating(@NotNull @Min(1) @Max(5) @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StarRatingDeserializer.class) Integer rating,@Size(max=1000) String content){}
    record Complaint(@NotBlank @Size(max=120) String reason,@Size(max=1000) String description){}
    private final OrderDeskService desk; private final OrderFeedback feedback; private final CurrentUserService auth;
    public OrderDeskController(OrderDeskService desk,OrderFeedback feedback,CurrentUserService auth){this.desk=desk;this.feedback=feedback;this.auth=auth;}
    @GetMapping("/mine") public ApiResponse<OrderDeskService.Page> mine(@RequestParam(defaultValue="ALL") String type,@RequestParam(defaultValue="ALL") String stage,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(desk.list(auth.requireUserId(),type,stage,page,size));}
    @GetMapping("/{kind}/{id}") public ApiResponse<OrderDeskService.Row> detail(@PathVariable String kind,@PathVariable long id){return ApiResponse.ok(desk.get(auth.requireUserId(),kind,id));}
    @PostMapping("/{kind}/{id}/review") public ApiResponse<Void> review(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody Rating form){feedback.review(auth.requireUserId(),kind,id,form.rating(),form.content());return ApiResponse.ok(null);}
    @PostMapping("/{kind}/{id}/complaint") public ApiResponse<Void> complain(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody Complaint form){feedback.complain(auth.requireUserId(),kind,id,form.reason(),form.description());return ApiResponse.ok(null);}
}
