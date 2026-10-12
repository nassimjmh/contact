package zip.nassim.contact.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.io.Serializable;

@Entity
public class Contact implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @Override
    public String toString() {
        return "Contacts [id=" + id + ", nom=" + nom + ", email=" + email + "]";
    }

    private String nom;
    private String email;
    private int note;

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public Contact() {
        super();
    }

    public Contact(String nom, String email, int note) {
        super();
        this.nom = nom;
        this.email = email;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}