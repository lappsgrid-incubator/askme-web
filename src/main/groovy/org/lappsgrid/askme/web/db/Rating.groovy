package org.lappsgrid.askme.web.db


import javax.persistence.Entity
import javax.persistence.Id

/**
 *
 */
@Entity
class Rating {

    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    Long id
//    @Column(name='uuid', unique = true)
    String uuid
    Integer rating

    protected Rating() {}

    public Rating(String uuid, Integer rating) {
        this.uuid = uuid
        this.rating = rating
    }

    String toString() {
        return uuid + '\t' + rating
    }
}
