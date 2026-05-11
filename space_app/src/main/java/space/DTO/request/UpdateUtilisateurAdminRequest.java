package space.DTO.request;

import jakarta.validation.constraints.Email;
import space.ENUM.TYPE_COMPTE;

public class UpdateUtilisateurAdminRequest {

    @Email(message = "Format d'email invalide")
    private String mail;

    private String password; // optionnel : vide = pas de changement

    private String lastname;
    private String firstname;
    private TYPE_COMPTE role;

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public TYPE_COMPTE getRole() { return role; }
    public void setRole(TYPE_COMPTE role) { this.role = role; }
}
