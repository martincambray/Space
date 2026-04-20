package space.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import space.MODEL.Role;

public class CreateUtilisateurRequest 
{
    @NotBlank(message = "Le mail est obligatoire")
    @Email(message = "Format d'email invalide")
    private String mail;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastname;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstname;

    @NotNull(message = "Le rôle est obligatoire")
    private RoleEnum role;

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public RoleEnum getRole() { return role; }
    public void setRole(RoleEnum role) { this.role = role; }
}
