package space.DTO.response;

import space.ENUM.TYPE_COMPTE;
import space.MODEL.Utilisateur;

public class UtilisateurResponse 
{
    private int id;
    private String mail;
    private String lastname;
    private String firstname;
    private TYPE_COMPTE role;
    private boolean suspended;

    public static UtilisateurResponse convert(Utilisateur u)
    {
        UtilisateurResponse resp = new UtilisateurResponse();
        resp.setId(u.getId());
        resp.setMail(u.getMail());
        resp.setLastname(u.getLastname());
        resp.setFirstname(u.getFirstname());
        resp.setRole(u.getRole());
        resp.setSuspended(u.isSuspended());
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

    public TYPE_COMPTE getRole() { return role; }
    public void setRole(TYPE_COMPTE role) { this.role = role; }

    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
}
