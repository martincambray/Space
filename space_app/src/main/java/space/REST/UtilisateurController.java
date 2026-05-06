package space.REST;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import space.DAO.IDAOUtilisateur;
import space.DTO.request.CreateUtilisateurRequest;
import space.DTO.request.UpdateMeRequest;
import space.DTO.request.UpdateUtilisateurAdminRequest;
import space.DTO.response.UtilisateurResponse;
import space.MODEL.Utilisateur;

@RestController
@RequestMapping("/api/utilisateur")
public class UtilisateurController {

    private final IDAOUtilisateur daoUtilisateur;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurController(IDAOUtilisateur daoUtilisateur, PasswordEncoder passwordEncoder) {
        this.daoUtilisateur = daoUtilisateur;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtilisateurResponse> findAll() {
        return this.daoUtilisateur.findAll().stream()
            .map(UtilisateurResponse::convert)
            .toList();
    }

    @GetMapping("/me")
    public UtilisateurResponse findMe(Principal principal) {
        return this.daoUtilisateur.findByMail(principal.getName())
            .map(UtilisateurResponse::convert)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurResponse create(@Valid @RequestBody CreateUtilisateurRequest request) {
        Utilisateur user = new Utilisateur();
        user.setMail(request.getMail());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        user.setLastname(request.getLastname());
        user.setFirstname(request.getFirstname());
        user.setRole(request.getRole());
        return UtilisateurResponse.convert(this.daoUtilisateur.save(user));
    }

    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMe(@Valid @RequestBody UpdateMeRequest request, Principal principal) {
        Utilisateur user = this.daoUtilisateur.findByMail(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        }
        if (request.getMail() != null && !request.getMail().isBlank()) {
            user.setMail(request.getMail());
        }
        this.daoUtilisateur.save(user);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUser(@PathVariable int id, @Valid @RequestBody UpdateUtilisateurAdminRequest request) {
        Utilisateur user = this.daoUtilisateur.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.getMail() != null && !request.getMail().isBlank()) {
            user.setMail(request.getMail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        }
        if (request.getLastname() != null && !request.getLastname().isBlank()) {
            user.setLastname(request.getLastname());
        }
        if (request.getFirstname() != null && !request.getFirstname().isBlank()) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getRole() != null) user.setRole(request.getRole());
        this.daoUtilisateur.save(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable int id) {
        if (!this.daoUtilisateur.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        this.daoUtilisateur.deleteById(id);
    }
}
