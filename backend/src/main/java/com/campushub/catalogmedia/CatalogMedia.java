package com.campushub.catalogmedia;

import com.campushub.common.BusinessException;
import com.campushub.goods.GoodsListingAccess;
import com.campushub.moderation.GovernanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service @Transactional
public class CatalogMedia {
    record Photo(long id,boolean published,String url){}
    record Content(String mime,byte[] bytes){}
    private record Parent(long owner,boolean visible,String status){}
    private final JdbcTemplate db;
    private final GoodsListingAccess goodsAccess;
    private final GovernanceService governance;
    public CatalogMedia(JdbcTemplate db, GoodsListingAccess goodsAccess, GovernanceService governance){
        this.db=db; this.goodsAccess=goodsAccess; this.governance=governance;
    }
    private static ResponseStatusException hidden(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"图片不可查看");}
    private Parent parent(String kind,long id,Long actor,boolean write){
        String q=switch(kind){
            case "GOODS"->"select seller_id,status,false from goods where id=?";
            case "SHOP"->"select owner_id,status,status='APPROVED' from shops where id=?";
            case "SERVICE_ITEM"->"select s.owner_id,i.status,(s.status='APPROVED' and i.status='PUBLISHED') from service_items i join shops s on s.id=i.shop_id where i.id=?";
            case "PROJECT_AD"->"select publisher_id,status,(status='APPROVED' and (expires_at is null or expires_at>current_timestamp)) from project_ads where id=?";
            default->throw hidden();};
        var rows=db.query(q+(write?" for update":""),(r,n)->new Parent(r.getLong(1),r.getBoolean(3),r.getString(2)),id);
        if(rows.isEmpty())throw hidden();var p=rows.get(0);boolean owner=actor!=null&&actor==p.owner();
        if(kind.equals("GOODS")) {
            if(GoodsListingAccess.DELETED.equals(p.status())) throw hidden(); // deleted binary URLs never resolve
            if(write) {
                if(!owner || !GoodsListingAccess.canEdit(p.status())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"不能修改此图片");
                governance.ensureCanPost(actor);
            } else {
                goodsAccess.requireReadable(id,p.owner(),p.status(),actor);
            }
            return new Parent(p.owner(),GoodsListingAccess.isPublic(p.status()),p.status());
        }
        if(write&&(!owner||Set.of("CLOSED","BLOCKED").contains(p.status())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"不能修改此图片");
        if(!write&&!owner&&!p.visible()) {
            boolean moderator=actor!=null && kind.equals("PROJECT_AD") && p.status().equals("PENDING_REVIEW") && db.queryForObject("select count(*) from user_roles ur join roles r on r.id=ur.role_id where ur.user_id=? and r.code in ('ROLE_SHOWCASE_ADMIN','ROLE_ADMIN','ROLE_MASTER_ADMIN')",Integer.class,actor)>0;
            if(!moderator)throw hidden();
        }
        return p;
    }
    private static Photo info(String kind,long id,long photo,boolean visible){return new Photo(photo,visible,"/api/catalog-media/"+kind+"/"+id+"/"+photo);}
    @Transactional(readOnly=true) public List<Photo> list(String kind,long id,Long actor){var p=parent(kind,id,actor,false);return db.query("select id from catalog_images where kind=? and object_id=? order by id",(r,n)->info(kind,id,r.getLong(1),p.visible()),kind,id);}
    @Transactional(readOnly=true) public Content get(String kind,long id,long photo,Long actor){parent(kind,id,actor,false);return db.query("select mime_type,payload from catalog_images where kind=? and object_id=? and id=?",(r,n)->new Content(r.getString(1),r.getBytes(2)),kind,id,photo).stream().findFirst().orElseThrow(CatalogMedia::hidden);}
    public Photo add(String kind,long id,long actor,String key,MultipartFile file){
        var p=parent(kind,id,actor,true);if(key==null||!key.matches("[A-Za-z0-9_-]{1,80}"))throw new BusinessException("上传标识无效");var image=CatalogImageCodec.read(file);
        var existing=db.query("select id,sha256 from catalog_images where kind=? and object_id=? and client_key=?",(r,n)->{if(!r.getString(2).equals(image.hash()))throw new BusinessException("重试图片不一致");return info(kind,id,r.getLong(1),p.visible());},kind,id,key);
        if(!existing.isEmpty())return existing.get(0);
        if(db.queryForObject("select count(*) from catalog_images where kind=? and object_id=?",Integer.class,kind,id)>=(kind.equals("SHOP")?1:3))throw new BusinessException("图片已满，请先移除一张");
        db.update("insert into catalog_images(kind,object_id,uploader_id,client_key,sha256,mime_type,payload) values(?,?,?,?,?,?,?)",kind,id,actor,key,image.hash(),image.type(),image.bytes());
        recheckAd(kind,id,p);long photo=db.queryForObject("select id from catalog_images where kind=? and object_id=? and client_key=?",Long.class,kind,id,key);return info(kind,id,photo,p.visible()&&!kind.equals("PROJECT_AD"));
    }
    public void remove(String kind,long id,long photo,long actor){var p=parent(kind,id,actor,true);if(db.update("delete from catalog_images where kind=? and object_id=? and id=?",kind,id,photo)>0)recheckAd(kind,id,p);}
    private void recheckAd(String kind,long id,Parent p){if(kind.equals("PROJECT_AD")&&p.status().equals("APPROVED"))db.update("update project_ads set status='PENDING_REVIEW',featured=false,featured_priority=0,review_note=null,reviewed_by=null,reviewed_at=null,published_at=null where id=?",id);}
}
