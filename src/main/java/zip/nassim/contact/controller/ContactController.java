package zip.nassim.contact.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zip.nassim.contact.dao.ContactRepository;
import zip.nassim.contact.dao.PersonRepository;
import zip.nassim.contact.entities.Contact;
import zip.nassim.contact.entities.Person;

import java.util.List;
import java.util.Optional;

@Controller
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PersonRepository personRepository;

    @GetMapping("/contacts")
    public String contacts(Model model) {
        List<Contact> contacts = contactRepository.findAll();
        model.addAttribute("contacts", contacts);
        return "liste";
    }

    @GetMapping("/ajout")
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

    @GetMapping("/delete")
    public String delete() {
        return "delete";
    }

    @PostMapping("/deleteSave")
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
}
