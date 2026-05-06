package space.REST;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.CONFIG.JwtUtils;
import space.DTO.request.AuthRequest;
import space.DTO.response.TokenResponse;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/auth")
    public TokenResponse auth(@RequestBody AuthRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            request.getMail(), request.getPassword()
        );
        authentication = this.authenticationManager.authenticate(authentication);
        return new TokenResponse(this.jwtUtils.generate(authentication));
    }
}
