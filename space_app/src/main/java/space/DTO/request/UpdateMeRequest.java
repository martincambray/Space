package space.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateMeRequest 
{
    @Email(message = "Format d'email invalide")
    private String mail;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
