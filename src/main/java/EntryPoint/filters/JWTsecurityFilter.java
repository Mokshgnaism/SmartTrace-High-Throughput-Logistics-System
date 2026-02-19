package EntryPoint.filters;

import EntryPoint.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jdk.dynalink.linker.support.SimpleLinkRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
public class JWTsecurityFilter extends OncePerRequestFilter {
    private JwtUtil jwtUtil;
    public JWTsecurityFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring(7);
        String userId =  jwtUtil.getUserId(token);
        String role =  jwtUtil.getRole(token);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId,null, List.of(new SimpleGrantedAuthority("ROLE_"+role))));
        filterChain.doFilter(request, response);
        return;
    }
}
