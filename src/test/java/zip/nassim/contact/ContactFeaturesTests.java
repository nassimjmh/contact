package zip.nassim.contact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zip.nassim.contact.dao.ContactRepository;
import zip.nassim.contact.dao.PersonRepository;
import zip.nassim.contact.entities.Contact;
import zip.nassim.contact.entities.Person;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ContactFeaturesTests {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
        personRepository.deleteAll();
    }

    @Test
    void testCrudPersonne() {
        // Ajout
        Person alice = new Person("Dupont", "Alice", "10 Rue de Paris");
        personRepository.save(alice);
        assertNotNull(alice.getId());

        // Lecture / Modification
        Optional<Person> found = personRepository.findById(alice.getId());
        assertTrue(found.isPresent());
        Person pers = found.get();
        pers.setAdresse("20 Avenue des Champs");
        personRepository.save(pers);

        Person updated = personRepository.findById(alice.getId()).orElseThrow();
        assertEquals("20 Avenue des Champs", updated.getAdresse());

        // Suppression
        personRepository.delete(updated);
        assertTrue(personRepository.findById(alice.getId()).isEmpty());
    }

    @Test
    void testCrudContactEtAssociationPersonne() {
        Person bob = personRepository.save(new Person("Martin", "Bob", "5 Rue Victor Hugo"));

        // Ajout contact associé
        Contact contact1 = new Contact("bob1@test.com", 14, bob);
        contactRepository.save(contact1);
        assertNotNull(contact1.getId());

        // Vérifier unicité email
        assertTrue(contactRepository.existsByEmail("bob1@test.com"));

        // Modification contact
        contact1.setNote(16);
        contactRepository.save(contact1);

        Contact reloaded = contactRepository.findById(contact1.getId()).orElseThrow();
        assertEquals(16, reloaded.getNote());
        assertEquals("bob1@test.com", reloaded.getEmail());
        assertEquals(bob.getId(), reloaded.getPerson().getId());

        // Suppression contact
        contactRepository.delete(reloaded);
        assertTrue(contactRepository.findById(contact1.getId()).isEmpty());
    }

    @Test
    void testAffichageProprietaireContact() {
        Person charlie = personRepository.save(new Person("Durand", "Charlie", "7 Boulevard Saint-Michel"));
        Contact contact = contactRepository.save(new Contact("charlie@contact.com", 18, charlie));
        entityManager.flush();
        entityManager.clear();

        Optional<Contact> found = contactRepository.findByEmail("charlie@contact.com");
        assertTrue(found.isPresent());
        assertNotNull(found.get().getPerson());
        assertEquals("Durand", found.get().getPerson().getNom());
        assertEquals("Charlie", found.get().getPerson().getPrenom());
        assertEquals("7 Boulevard Saint-Michel", found.get().getPerson().getAdresse());
    }

    @Test
    void testPersonnesSansContact() {
        Person sansContact1 = personRepository.save(new Person("Solo", "Luc", "Nulle part"));
        Person sansContact2 = personRepository.save(new Person("Ermite", "Jean", "Montagne"));
        Person avecContact = personRepository.save(new Person("Social", "Paul", "Ville"));
        contactRepository.save(new Contact("paul@contact.com", 12, avecContact));

        entityManager.flush();
        entityManager.clear();

        List<Person> sansContacts = personRepository.findPersonsWithNoContacts();
        assertEquals(2, sansContacts.size());
        assertTrue(sansContacts.stream().anyMatch(p -> p.getId().equals(sansContact1.getId())));
        assertTrue(sansContacts.stream().anyMatch(p -> p.getId().equals(sansContact2.getId())));
        assertFalse(sansContacts.stream().anyMatch(p -> p.getId().equals(avecContact.getId())));
    }

    @Test
    void testMeilleureMoyenne() {
        Person p1 = personRepository.save(new Person("Un", "Pierre", "Paris"));
        Person p2 = personRepository.save(new Person("Deux", "Marie", "Lyon"));

        // P1 a 2 contacts : note 10 et 14 -> moyenne = 12
        contactRepository.save(new Contact("c1@test.com", 10, p1));
        contactRepository.save(new Contact("c2@test.com", 14, p1));

        // P2 a 2 contacts : note 18 et 16 -> moyenne = 17
        contactRepository.save(new Contact("c3@test.com", 18, p2));
        contactRepository.save(new Contact("c4@test.com", 16, p2));

        entityManager.flush();
        entityManager.clear();

        List<Person> personnes = personRepository.findAllWithContacts();
        Person best = personnes.stream()
                .filter(p -> p.getContacts() != null && !p.getContacts().isEmpty())
                .max(Comparator.comparingDouble(p -> p.getContacts().stream()
                        .mapToInt(Contact::getNote)
                        .average()
                        .orElse(0.0)))
                .orElse(null);

        assertNotNull(best);
        assertEquals(p2.getId(), best.getId());
        double avg = best.getContacts().stream().mapToInt(Contact::getNote).average().orElse(0.0);
        assertEquals(17.0, avg);
    }

    @Test
    void testRechercheParNoteEtMalNotes() {
        contactRepository.save(new Contact("bad1@test.com", 4));
        contactRepository.save(new Contact("bad2@test.com", 8));
        contactRepository.save(new Contact("avg@test.com", 10));
        contactRepository.save(new Contact("good@test.com", 15));
        contactRepository.save(new Contact("top@test.com", 18));

        // Note spécifique = 15
        List<Contact> notes15 = contactRepository.findByNote(15);
        assertEquals(1, notes15.size());
        assertEquals("good@test.com", notes15.get(0).getEmail());

        // Mal notés (< 10)
        List<Contact> malNotes = contactRepository.findByNoteLessThan(10);
        assertEquals(2, malNotes.size());
        assertTrue(malNotes.stream().allMatch(c -> c.getNote() < 10));
    }
}
