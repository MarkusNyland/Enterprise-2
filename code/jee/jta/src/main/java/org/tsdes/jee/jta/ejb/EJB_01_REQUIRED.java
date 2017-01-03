package org.tsdes.jee.jta.ejb;


import org.tsdes.jee.jta.data.Foo;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED) //this is the default
public class EJB_01_REQUIRED {

    @PersistenceContext
    private EntityManager em;

    public void createFoo(String name){
        Foo foo = new Foo(name);
        em.persist(foo);
    }
}
