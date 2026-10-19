package zip.nassim.contact.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zip.nassim.contact.dao.PersonRepository;
import zip.nassim.contact.entities.Contact;
import zip.nassim.contact.entities.Person;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class PersonController {

    @Autowired
    private PersonRepository personRepository;

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

    @PostMapping("/personnes/deleteSave")
    public String deletePersonneSave(@RequestParam Long id) {
        personRepository.findById(id).ifPresent(personRepository::delete);
        return "redirect:/personnes";
    }

    @GetMapping("/personnes/sans-contact")
    public String personnesSansContact(Model model) {
        List<Person> sansContact = personRepository.findPersonsWithNoContacts();
        model.addAttribute("personnes", sansContact);
        return "personnes-sans-contact";
    }

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
