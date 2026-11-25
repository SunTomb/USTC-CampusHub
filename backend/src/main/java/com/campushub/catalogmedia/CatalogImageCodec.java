package com.campushub.catalogmedia;

import com.campushub.common.BusinessException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Decode before publishing, strip all original metadata and never use upload filenames. */
final class CatalogImageCodec {
    static final int MAX=5*1024*1024;
    record Image(String type, String hash,byte[] bytes){}
    static Image read(MultipartFile file){
        if(file==null||file.isEmpty()||file.getSize()>MAX) throw new BusinessException("图片不能超过 5 MiB");
        try(var raw=file.getInputStream()){
            byte[] bytes=raw.readNBytes(MAX+1);if(bytes.length>MAX)throw new BusinessException("图片过大");
            try(var source=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
                var readers=ImageIO.getImageReaders(source);if(!readers.hasNext())throw new BusinessException("请选择 JPG 或 PNG 图片");
                var reader=readers.next();try {reader.setInput(source,true,true);String fmt=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if(!fmt.equals("png")&&!fmt.equals("jpeg"))throw new BusinessException("请选择 JPG 或 PNG 图片");
                    String mime="image/"+fmt;String supplied=file.getContentType();if(supplied!=null&&!supplied.equals("application/octet-stream")&&!supplied.equalsIgnoreCase(mime))throw new BusinessException("图片格式不匹配");
                    long w=reader.getWidth(0),h=reader.getHeight(0);if(w<=0||h<=0||w>8192||h>8192||w*h>8000000)throw new BusinessException("图片尺寸过大");
                    var rendered=reader.read(0);var out=new ByteArrayOutputStream();if(!ImageIO.write(rendered,fmt,out)||out.size()>MAX)throw new BusinessException("图片过大，请压缩后上传");
                    return new Image(mime,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),out.toByteArray());
                } finally{reader.dispose();}
            }
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException("无法读取图片，请重新选择");}
    }
}
