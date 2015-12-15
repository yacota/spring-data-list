/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hello.model.generic;

import org.springframework.data.annotation.Id;

public abstract class AbstractContent {
     
    @Id
    protected final Long id;    
     
    public AbstractContent(Long id) {
        this.id       = id;
    }

    public Long getId() {
        return id;
    }
    
}
