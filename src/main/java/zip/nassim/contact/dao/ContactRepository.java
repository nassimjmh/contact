package zip.nassim.contact.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zip.nassim.contact.entities.Contact;

public interface ContactRepository extends JpaRepository<Contact,Long> {

}
