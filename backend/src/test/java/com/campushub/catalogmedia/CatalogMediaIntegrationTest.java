package com.campushub.catalogmedia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
class CatalogMediaIntegrationTest {
 @Autowired CatalogMedia media; @Autowired JdbcTemplate db;
 private MockMultipartFile photo() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);return new MockMultipartFile("file","sample.png","image/png",out.toByteArray());}
 @Test void separateCoverAndProductBoundedIdempotentAndPrivateWhenPaused() throws Exception {var f=photo();var cover=media.add("SHOP",1,3,"key",f);assertThat(media.add("SHOP",1,3,"key",f).id()).isEqualTo(cover.id());assertThatThrownBy(()->media.add("SHOP",1,3,"key2",f)).isInstanceOf(RuntimeException.class);var product=media.add("SERVICE_ITEM",1,3,"item",f);assertThat(media.list("SHOP",1,null)).hasSize(1);assertThat(media.get("SERVICE_ITEM",1,product.id(),null).bytes()).isNotEmpty();assertThatThrownBy(()->media.remove("SHOP",1,cover.id(),2)).isInstanceOf(RuntimeException.class);db.update("update shops set status='PAUSED' where id=1");assertThatThrownBy(()->media.get("SERVICE_ITEM",1,product.id(),null)).isInstanceOf(RuntimeException.class);assertThat(media.get("SHOP",1,cover.id(),3L).bytes()).isNotEmpty();}
 @Test void changedAdGoesBackToReviewAndRejectsUnknownContent() throws Exception{var f=photo();var p=media.add("PROJECT_AD",1,1,"new",f);assertThat(p.published()).isFalse();assertThat(db.queryForObject("select status from project_ads where id=1",String.class)).isEqualTo("PENDING_REVIEW");assertThatThrownBy(()->media.list("PROJECT_AD",1,null)).isInstanceOf(RuntimeException.class);assertThatThrownBy(()->media.add("PROJECT_AD",1,1,"bad",new MockMultipartFile("file","x.png","image/png",new byte[]{1}))).isInstanceOf(RuntimeException.class);assertThatThrownBy(()->media.list("TASK",1,1L)).isInstanceOf(RuntimeException.class);}
}
