package com.tamaka.cuentas.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.cuentas.repository.CuentaRepository;

@Service
public class CuentaUserDetailsService implements UserDetailsService {

    private final CuentaRepository cuentaRepository;

    public CuentaUserDetailsService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return cuentaRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .map(CuentaPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }
}
