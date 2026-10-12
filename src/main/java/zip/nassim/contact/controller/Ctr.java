package zip.nassim.contact.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zip.nassim.contact.dao.ContactRepository;
import zip.nassim.contact.entities.Contact;

import java.util.List;
import java.util.Optional;

@Controller
public class Ctr {
    @Autowired
    ContactRepository contactRepository;
    @RequestMapping("/contacts")
    public String contacts(Model model) throws InterruptedException{
        List<Contact> contacts = contactRepository.findAll();
        model.addAttribute("contacts",contacts);
        return "liste";
    }
    @RequestMapping("/save")
    public String save(@RequestParam String nom,@RequestParam String email,@RequestParam String note,Model model) throws InterruptedException{
        contactRepository.save(new Contact(nom,email,Integer.parseInt(note)));
        return contacts(model);
    }
    @RequestMapping("/ajout")
    public String ajout(){
        return "add";
    }
    @RequestMapping("/delete")
    public String delete(){
        return "delete";
    }
    @RequestMapping("/deleteSave")
    public String deleteSave(@RequestParam Long id){
        Optional<Contact> contact = contactRepository.findById(id);
        if (contact.isPresent()){
            contactRepository.deleteById(id);
        }
        return "delete";
    }
}
