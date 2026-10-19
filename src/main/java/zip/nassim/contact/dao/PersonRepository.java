package zip.nassim.contact.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import zip.nassim.contact.entities.Person;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    @Query("SELECT p FROM Person p WHERE p.contacts IS EMPTY")
    List<Person> findPersonsWithNoContacts();


    @Query("SELECT DISTINCT p FROM Person p LEFT JOIN FETCH p.contacts")
    List<Person> findAllWithContacts();
}
