package com.jhanp.psraerobicsbackend.modules.role.entity;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "rol")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Sin @Enumerated: RoleNameConverter se aplica solo (autoApply) y guarda el valor en español.
    @Column(name = "nombre", nullable = false, unique = true, columnDefinition = "rol_nombre_enum")
    private RoleName name;
}
