/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hello.model.generic;

import java.util.Map;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author jllach
 */
public class LazyRelation<T extends AbstractContent> {
    
    private static final long serialVersionUID = 8397043489764785782L;

    @Field("_CONTENT")
    @DBRef(lazy = true)
    private T   content;
    
    @Field("_ATTRIBUTES")
    private Map<String, Object>  attributes;

    public LazyRelation (Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public T    getContent() {
        return content;
    }
    
    // TEST
    public void setContent(T content) {
        this.content = content;
    }

}