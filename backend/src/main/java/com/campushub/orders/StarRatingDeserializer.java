package com.campushub.orders;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
/** Do not coerce 4.9 or strings to whole stars. */
public class StarRatingDeserializer extends JsonDeserializer<Integer> {
    @Override public Integer deserialize(JsonParser p,DeserializationContext context)throws IOException {
        if(p.currentToken()!=JsonToken.VALUE_NUMBER_INT)return context.reportInputMismatch(Integer.class,"请选择 1 至 5 个整星");
        return p.getIntValue();
    }
}
