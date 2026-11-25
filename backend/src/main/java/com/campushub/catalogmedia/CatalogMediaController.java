package com.campushub.catalogmedia;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/catalog-media/{kind}/{id}")
public class CatalogMediaController {
    private final CurrentUserService auth;private final CatalogMedia media;
    public CatalogMediaController(CurrentUserService auth,CatalogMedia media){this.auth=auth;this.media=media;}
    @GetMapping public ResponseEntity<ApiResponse<List<CatalogMedia.Photo>>> list(@PathVariable String kind,@PathVariable long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(media.list(kind,id,auth.optionalUserId().orElse(null))));}
    @GetMapping("/{photo}") public ResponseEntity<byte[]> read(@PathVariable String kind,@PathVariable long id,@PathVariable long photo){var file=media.get(kind,id,photo,auth.optionalUserId().orElse(null));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(file.mime())).header("X-Content-Type-Options","nosniff").body(file.bytes());}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<CatalogMedia.Photo> add(@PathVariable String kind,@PathVariable long id,@RequestParam String clientRequestId,@RequestPart MultipartFile file){return ApiResponse.ok(media.add(kind,id,auth.requireUserId(),clientRequestId,file));}
    @DeleteMapping("/{photo}") public ApiResponse<Void> remove(@PathVariable String kind,@PathVariable long id,@PathVariable long photo){media.remove(kind,id,photo,auth.requireUserId());return ApiResponse.ok(null);}
}
