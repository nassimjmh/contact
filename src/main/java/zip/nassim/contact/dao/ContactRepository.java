package zip.nassim.contact.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zip.nassim.contact.entities.Contact;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Optional<Contact> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Contact> findByNote(int note);

    List<Contact> findByNoteLessThan(int note);

    List<Contact> findByPersonId(Long personId);
}
