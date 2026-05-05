package space.DTO.response;

import space.MODEL.Role;
import space.MODEL.Utilisateur;

public class UtilisateurResponse 
{
    private int id;
    private String mail;
    private String lastname;
    private String firstname;
    private Role role;

    public static UtilisateurResponse convert(Utilisateur u) 
    {
        UtilisateurResponse resp = new UtilisateurResponse();
        resp.setId(u.getId());
        resp.setMail(u.getMail());
        resp.setLastname(u.getLastname());
        resp.setFirstname(u.getFirstname());
        resp.setRole(u.getRole());
        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
