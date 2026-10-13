package zip.nassim.contact.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zip.nassim.contact.dao.ContactRepository;
import zip.nassim.contact.dao.PersonRepository;
import zip.nassim.contact.entities.Contact;
import zip.nassim.contact.entities.Person;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class Ctr {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PersonRepository personRepository;

    // ==========================================
    // PAGE D'ACCUEIL
    // ==========================================
    @RequestMapping("/")
    public String home() {
        return "index";
    }

    // ==========================================
    // GESTION DES CONTACTS
    // ==========================================
    @RequestMapping("/contacts")
    public String contacts(Model model) {
        List<Contact> contacts = contactRepository.findAll();
        model.addAttribute("contacts", contacts);
        return "liste";
    }

    @RequestMapping("/ajout")
    public String ajout(Model model) {
        List<Person> personnes = personRepository.findAll();
        model.addAttribute("personnes", personnes);
        return "add";
    }

    @PostMapping("/save")
    public String save(@RequestParam String email,
                       @RequestParam int note,
                       @RequestParam(required = false) Long personId,
                       Model model) {
        // Vérification de l'unicité de l'email
        if (contactRepository.existsByEmail(email)) {
            model.addAttribute("errorMessage", "Erreur : Un contact avec l'adresse email '" + email + "' existe déjà !");
            model.addAttribute("personnes", personRepository.findAll());
            model.addAttribute("email", email);
            model.addAttribute("note", note);
            model.addAttribute("selectedPersonId", personId);
            return "add";
        }

        Contact contact = new Contact(email, note);
        if (personId != null && personId > 0) {
            personRepository.findById(personId).ifPresent(contact::setPerson);
        }

        contactRepository.save(contact);
        return "redirect:/contacts";
    }

    @GetMapping("/contacts/edit")
    public String editContact(@RequestParam Long id, Model model) {
        Optional<Contact> contactOpt = contactRepository.findById(id);
        if (contactOpt.isEmpty()) {
            return "redirect:/contacts";
        }
        model.addAttribute("contact", contactOpt.get());
        model.addAttribute("personnes", personRepository.findAll());
        return "edit-contact";
    }

    @PostMapping("/contacts/update")
    public String updateContact(@RequestParam Long id,
                                @RequestParam String email,
                                @RequestParam int note,
                                @RequestParam(required = false) Long personId,
                                Model model) {
        Optional<Contact> contactOpt = contactRepository.findById(id);
        if (contactOpt.isEmpty()) {
            return "redirect:/contacts";
        }

        Contact contact = contactOpt.get();

        // Vérifier si l'email a changé et s'il est déjà pris par un autre contact
        if (!contact.getEmail().equalsIgnoreCase(email)) {
            Optional<Contact> existing = contactRepository.findByEmail(email);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                model.addAttribute("errorMessage", "Erreur : L'adresse email '" + email + "' est déjà utilisée par un autre contact !");
                model.addAttribute("contact", contact);
                model.addAttribute("personnes", personRepository.findAll());
                return "edit-contact";
            }
        }

        contact.setEmail(email);
        contact.setNote(note);

        if (personId != null && personId > 0) {
            Optional<Person> personOpt = personRepository.findById(personId);
            contact.setPerson(personOpt.orElse(null));
        } else {
            contact.setPerson(null);
        }

        contactRepository.save(contact);
        return "redirect:/contacts";
    }

    @RequestMapping("/delete")
    public String delete() {
        return "delete";
    }

    @RequestMapping("/deleteSave")
    public String deleteSave(@RequestParam Long id,
                             @RequestParam(required = false) String redirectUrl,
                             Model model) {
        Optional<Contact> contact = contactRepository.findById(id);
        if (contact.isPresent()) {
            contactRepository.deleteById(id);
            model.addAttribute("message", "Le contact avec l'ID " + id + " a été supprimé avec succès.");
        } else {
            model.addAttribute("errorMessage", "Aucun contact trouvé avec l'identifiant " + id + ".");
        }

        if ("contacts".equals(redirectUrl)) {
            return "redirect:/contacts";
        }
        return "delete";
    }

    @RequestMapping("/contacts/delete")
    public String deleteContactDirect(@RequestParam Long id) {
        contactRepository.deleteById(id);
        return "redirect:/contacts";
    }

    // ==========================================
    // RECHERCHE DE CONTACTS (MAL NOTES / NOTE SPECIFIQUE)
    // ==========================================
    @GetMapping("/contacts/recherche")
    public String rechercheContacts(@RequestParam(required = false) String type,
                                    @RequestParam(required = false) Integer note,
                                    @RequestParam(required = false) Integer seuil,
                                    Model model) {
        List<Contact> resultats = null;
        String critere = null;

        if ("specifique".equals(type) && note != null) {
            resultats = contactRepository.findByNote(note);
            critere = "Contacts ayant exactement la note de " + note;
            model.addAttribute("searchNote", note);
        } else if ("mal-notes".equals(type)) {
            int seuilEffectif = (seuil != null) ? seuil : 10;
            resultats = contactRepository.findByNoteLessThan(seuilEffectif);
            critere = "Contacts mal notés (note strictement inférieure à " + seuilEffectif + ")";
            model.addAttribute("searchSeuil", seuilEffectif);
        }

        model.addAttribute("type", type);
        model.addAttribute("resultats", resultats);
        model.addAttribute("critere", critere);
        return "recherche-contacts";
    }

    // ==========================================
    // PROPRIETAIRE D'UN CONTACT DONNE
    // ==========================================
    @GetMapping("/contacts/proprietaire")
    public String afficherProprietaire(@RequestParam(required = false) Long contactId,
                                       @RequestParam(required = false) String email,
                                       Model model) {
        Contact contactTrouve = null;

        if (contactId != null) {
            contactTrouve = contactRepository.findById(contactId).orElse(null);
            model.addAttribute("rechercheId", contactId);
        } else if (email != null && !email.trim().isEmpty()) {
            contactTrouve = contactRepository.findByEmail(email.trim()).orElse(null);
            model.addAttribute("rechercheEmail", email.trim());
        }

        if (contactTrouve != null) {
            model.addAttribute("contact", contactTrouve);
            Person proprietaire = contactTrouve.getPerson();
            model.addAttribute("proprietaire", proprietaire);
            if (proprietaire != null) {
                List<Contact> autresContacts = contactRepository.findByPersonId(proprietaire.getId());
                model.addAttribute("autresContacts", autresContacts);
            }
        } else if (contactId != null || (email != null && !email.trim().isEmpty())) {
            model.addAttribute("introuvable", true);
        }

        return "proprietaire-contact";
    }

    // ==========================================
    // GESTION DES PERSONNES
    // ==========================================
    @GetMapping("/personnes")
    public String listePersonnes(Model model) {
        List<Person> personnes = personRepository.findAllWithContacts();
        model.addAttribute("personnes", personnes);
        return "liste-personnes";
    }

    @GetMapping("/personnes/ajout")
    public String ajoutPersonne() {
        return "add-person";
    }

    @PostMapping("/personnes/save")
    public String savePersonne(@RequestParam String nom,
                               @RequestParam String prenom,
                               @RequestParam String adresse) {
        Person person = new Person(nom, prenom, adresse);
        personRepository.save(person);
        return "redirect:/personnes";
    }

    @GetMapping("/personnes/edit")
    public String editPersonne(@RequestParam Long id, Model model) {
        Optional<Person> personOpt = personRepository.findById(id);
        if (personOpt.isEmpty()) {
            return "redirect:/personnes";
        }
        model.addAttribute("personne", personOpt.get());
        return "edit-person";
    }

    @PostMapping("/personnes/update")
    public String updatePersonne(@RequestParam Long id,
                                 @RequestParam String nom,
                                 @RequestParam String prenom,
                                 @RequestParam String adresse) {
        Optional<Person> personOpt = personRepository.findById(id);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            person.setNom(nom);
            person.setPrenom(prenom);
            person.setAdresse(adresse);
            personRepository.save(person);
        }
        return "redirect:/personnes";
    }

    @GetMapping("/personnes/supprimer")
    public String supprimerPersonnePage() {
        return "delete-person";
    }

    @RequestMapping("/personnes/deleteSave")
    public String deletePersonneSave(@RequestParam Long id,
                                     @RequestParam(required = false) String redirectUrl,
                                     Model model) {
        Optional<Person> personOpt = personRepository.findById(id);
        if (personOpt.isPresent()) {
            personRepository.delete(personOpt.get());
            model.addAttribute("message", "La personne avec l'ID " + id + " a été supprimée avec succès.");
        } else {
            model.addAttribute("errorMessage", "Aucune personne trouvée avec l'identifiant " + id + ".");
        }

        if ("personnes".equals(redirectUrl)) {
            return "redirect:/personnes";
        }
        return "delete-person";
    }

    @RequestMapping("/personnes/delete")
    public String deletePersonneDirect(@RequestParam Long id) {
        personRepository.findById(id).ifPresent(personRepository::delete);
        return "redirect:/personnes";
    }

    // ==========================================
    // RECHERCHE DES PERSONNES SANS CONTACT
    // ==========================================
    @GetMapping("/personnes/sans-contact")
    public String personnesSansContact(Model model) {
        List<Person> sansContact = personRepository.findPersonsWithNoContacts();
        model.addAttribute("personnes", sansContact);
        return "personnes-sans-contact";
    }

    // ==========================================
    // PERSONNE AVEC LA MOYENNE LA PLUS ELEVEE
    // ==========================================
    @GetMapping("/personnes/meilleure-moyenne")
    public String meilleureMoyenne(Model model) {
        List<Person> personnes = personRepository.findAllWithContacts();

        List<PersonStats> statsList = new ArrayList<>();
        for (Person p : personnes) {
            if (p.getContacts() != null && !p.getContacts().isEmpty()) {
                double avg = p.getContacts().stream()
                        .mapToInt(Contact::getNote)
                        .average()
                        .orElse(0.0);
                statsList.add(new PersonStats(p, avg, p.getContacts().size()));
            }
        }

        statsList.sort(Comparator.comparingDouble(PersonStats::getAverageNote).reversed());

        if (!statsList.isEmpty()) {
            PersonStats best = statsList.get(0);
            model.addAttribute("best", best);
            model.addAttribute("bestContacts", best.getPerson().getContacts());
        }

        model.addAttribute("statsList", statsList);
        return "meilleure-moyenne";
    }

    // Classe DTO pour les statistiques de moyennes
    public static class PersonStats {
        private final Person person;
        private final double averageNote;
        private final int contactCount;

        public PersonStats(Person person, double averageNote, int contactCount) {
            this.person = person;
            this.averageNote = averageNote;
            this.contactCount = contactCount;
        }

        public Person getPerson() {
            return person;
        }

        public double getAverageNote() {
            return averageNote;
        }

        public String getFormattedAverage() {
            return String.format(Locale.US, "%.2f", averageNote);
        }

        public int getContactCount() {
            return contactCount;
        }
    }
}
