package com.perurecicla.service;

import com.perurecicla.model.Canje;
import com.perurecicla.model.Recompensa;
import com.perurecicla.model.Usuario;

import com.perurecicla.repository.CanjeRepository;
import com.perurecicla.repository.RecompensaRepository;
import com.perurecicla.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PERU RECICLA - PASO 86
 *
 * Pruebas automaticas de la cancelacion de canjes.
 *
 * Verifica:
 *
 * 1. Cancelacion correcta.
 * 2. Devolucion de puntos originales.
 * 3. Restitucion de stock.
 * 4. Registro de auditoria.
 * 5. Proteccion de tickets entregados.
 * 6. Proteccion de tickets cancelados.
 * 7. Permisos administrativos.
 * 8. Validacion de motivos.
 * 9. Limites de puntos y stock.
 *
 * Utiliza Mockito.
 * NO modifica la base de datos MySQL.
 */
@ExtendWith(MockitoExtension.class)
class CancelacionCanjeServiceTest {

    // =====================================
    // DATOS DE PRUEBA
    // =====================================

    private static final String CODIGO_TICKET =
        "TK-PRUEBA-001";

    private static final String CORREO_ADMIN =
        "admin.prueba@perurecicla.test";

    private static final String MOTIVO =
        "Cancelacion solicitada por el vecino";

    // =====================================
    // REPOSITORIOS SIMULADOS
    // =====================================

    @Mock
    private CanjeRepository canjeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RecompensaRepository recompensaRepository;

    @Mock
    private EntityManager entityManager;

    // =====================================
    // SERVICIO QUE VAMOS A PROBAR
    // =====================================

    @InjectMocks
    private CancelacionCanjeService cancelacionCanjeService;

    // =====================================
    // PRUEBA 01
    //
    // CANCELACION CORRECTA
    // =====================================

    @Test
    void cancelarPendienteDevuelvePuntosYStock() {

        Usuario administrador =
            crearAdministradorActivo();

        Usuario vecino =
            crearVecino(
                new BigDecimal("350.00")
            );

        Recompensa recompensa =
            crearRecompensa(9);

        // El costo actual de la recompensa
        // es diferente del costo historico.
        //
        // El ticket costo originalmente 150.
        // Aunque ahora cueste 999, solo
        // deben devolverse 150 puntos.

        recompensa.setCosto_puntos(999);

        Canje canje =
            crearCanje(
                Canje.EstadoCanje.pendiente,
                vecino,
                recompensa,
                150
            );

        simularAdministrador(administrador);

        simularTicket(canje);

        // EJECUTAR

        cancelacionCanjeService.cancelarCanje(
            CODIGO_TICKET,
            "  " + MOTIVO + "  ",
            CORREO_ADMIN
        );

        // VERIFICAR PUNTOS

        assertEquals(
            new BigDecimal("500.00"),
            vecino.getPuntos_acumulados()
        );

        // VERIFICAR STOCK

        assertEquals(
            10,
            recompensa.getStock()
        );

        // VERIFICAR ESTADO

        assertEquals(
            Canje.EstadoCanje.cancelado,
            canje.getEstado()
        );

        // VERIFICAR AUDITORIA

        assertEquals(
            MOTIVO,
            canje.getMotivo_cancelacion()
        );

        assertNotNull(
            canje.getFecha_cancelacion()
        );

        assertEquals(
            administrador.getId_usuario(),
            canje.getId_administrador_cancelacion()
        );

        // VERIFICAR GUARDADO

        verify(
            usuarioRepository,
            times(1)
        ).save(vecino);

        verify(
            recompensaRepository,
            times(1)
        ).save(recompensa);

        verify(
            canjeRepository,
            times(1)
        ).save(canje);

        // VERIFICAR BLOQUEOS

        verify(entityManager).refresh(
            canje,
            LockModeType.PESSIMISTIC_WRITE
        );

        verify(entityManager).refresh(
            vecino,
            LockModeType.PESSIMISTIC_WRITE
        );

        verify(entityManager).refresh(
            recompensa,
            LockModeType.PESSIMISTIC_WRITE
        );
    }

    // =====================================
    // PRUEBA 02
    //
    // PROHIBIR CANCELAR ENTREGADOS
    // =====================================

    @Test
    void noPermiteCancelarCanjeEntregado() {

        Usuario administrador =
            crearAdministradorActivo();

        Usuario vecino =
            crearVecino(
                new BigDecimal("350.00")
            );

        Recompensa recompensa =
            crearRecompensa(9);

        Canje canje =
            crearCanje(
                Canje.EstadoCanje.entregado,
                vecino,
                recompensa,
                150
            );

        simularAdministrador(administrador);

        simularTicket(canje);

        // DEBE RECHAZAR LA OPERACION

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        // NO DEBE DEVOLVER RECURSOS

        assertEquals(
            new BigDecimal("350.00"),
            vecino.getPuntos_acumulados()
        );

        assertEquals(
            9,
            recompensa.getStock()
        );

        assertEquals(
            Canje.EstadoCanje.entregado,
            canje.getEstado()
        );

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 03
    //
    // PROHIBIR CANCELACION REPETIDA
    // =====================================

    @Test
    void noPermiteCancelarDosVeces() {

        Usuario administrador =
            crearAdministradorActivo();

        Usuario vecino =
            crearVecino(
                new BigDecimal("500.00")
            );

        Recompensa recompensa =
            crearRecompensa(10);

        // Este ticket YA esta cancelado.

        Canje canje =
            crearCanje(
                Canje.EstadoCanje.cancelado,
                vecino,
                recompensa,
                150
            );

        simularAdministrador(administrador);

        simularTicket(canje);

        // UN NUEVO INTENTO DEBE FALLAR

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        // EL SALDO Y EL STOCK NO CAMBIAN

        assertEquals(
            new BigDecimal("500.00"),
            vecino.getPuntos_acumulados()
        );

        assertEquals(
            10,
            recompensa.getStock()
        );

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 04
    //
    // VECINO NO PUEDE CANCELAR
    // =====================================

    @Test
    void vecinoNoPuedeCancelarCanjes() {

        Usuario usuario =
            crearAdministradorActivo();

        // Simular cuenta con rol de vecino.

        usuario.setRol(
            Usuario.Rol.ROLE_vecino
        );

        simularAdministrador(usuario);

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        // NO DEBE CONSULTAR EL TICKET

        verify(
            canjeRepository,
            never()
        ).buscarPorCodigoTicket(any());

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 05
    //
    // ADMINISTRADOR INACTIVO
    // =====================================

    @Test
    void administradorInactivoNoPuedeCancelar() {

        Usuario administrador =
            crearAdministradorActivo();

        administrador.setEstado(
            Usuario.Estado.inactivo
        );

        simularAdministrador(administrador);

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        verify(
            canjeRepository,
            never()
        ).buscarPorCodigoTicket(any());

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 06
    //
    // MOTIVO OBLIGATORIO
    // =====================================

    @Test
    void rechazaMotivoDemasiadoCorto() {

        Usuario administrador =
            crearAdministradorActivo();

        simularAdministrador(administrador);

        assertThrows(
            IllegalArgumentException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                "Corto",
                CORREO_ADMIN
            )
        );

        verify(
            canjeRepository,
            never()
        ).buscarPorCodigoTicket(any());

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 07
    //
    // TICKET INEXISTENTE
    // =====================================

    @Test
    void rechazaTicketInexistente() {

        Usuario administrador =
            crearAdministradorActivo();

        simularAdministrador(administrador);

        when(
            canjeRepository.buscarPorCodigoTicket(
                CODIGO_TICKET
            )
        ).thenReturn(
            Optional.empty()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 08
    //
    // NO SUPERAR SALDO MAXIMO
    // =====================================

    @Test
    void noPermiteReembolsoQueExcedeSaldoMaximo() {

        Usuario administrador =
            crearAdministradorActivo();

        Usuario vecino =
            crearVecino(
                new BigDecimal("99999990.00")
            );

        Recompensa recompensa =
            crearRecompensa(9);

        Canje canje =
            crearCanje(
                Canje.EstadoCanje.pendiente,
                vecino,
                recompensa,
                150
            );

        simularAdministrador(administrador);

        simularTicket(canje);

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        assertEquals(
            new BigDecimal("99999990.00"),
            vecino.getPuntos_acumulados()
        );

        assertEquals(
            9,
            recompensa.getStock()
        );

        assertEquals(
            Canje.EstadoCanje.pendiente,
            canje.getEstado()
        );

        verificarSinGuardados();
    }

    // =====================================
    // PRUEBA 09
    //
    // NO PERMITIR DESBORDAMIENTO DE STOCK
    // =====================================

    @Test
    void noPermiteDesbordamientoDeStock() {

        Usuario administrador =
            crearAdministradorActivo();

        Usuario vecino =
            crearVecino(
                new BigDecimal("350.00")
            );

        Recompensa recompensa =
            crearRecompensa(
                Integer.MAX_VALUE
            );

        Canje canje =
            crearCanje(
                Canje.EstadoCanje.pendiente,
                vecino,
                recompensa,
                150
            );

        simularAdministrador(administrador);

        simularTicket(canje);

        assertThrows(
            IllegalStateException.class,
            () -> cancelacionCanjeService.cancelarCanje(
                CODIGO_TICKET,
                MOTIVO,
                CORREO_ADMIN
            )
        );

        assertEquals(
            new BigDecimal("350.00"),
            vecino.getPuntos_acumulados()
        );

        assertEquals(
            Integer.MAX_VALUE,
            recompensa.getStock()
        );

        assertEquals(
            Canje.EstadoCanje.pendiente,
            canje.getEstado()
        );

        verificarSinGuardados();
    }

    // =====================================
    // METODOS AUXILIARES
    // =====================================

    private Usuario crearAdministradorActivo() {

        Usuario administrador = new Usuario();

        administrador.setId_usuario(1);

        administrador.setNombre("Administrador");

        administrador.setApellido("Prueba");

        administrador.setCorreo(CORREO_ADMIN);

        administrador.setRol(
            Usuario.Rol.ROLE_administrador
        );

        administrador.setEstado(
            Usuario.Estado.activo
        );

        return administrador;
    }

    private Usuario crearVecino(
            BigDecimal puntos) {

        Usuario vecino = new Usuario();

        vecino.setId_usuario(2);

        vecino.setNombre("Vecino");

        vecino.setApellido("Prueba");

        vecino.setRol(
            Usuario.Rol.ROLE_vecino
        );

        vecino.setEstado(
            Usuario.Estado.activo
        );

        vecino.setPuntos_acumulados(
            puntos
        );

        return vecino;
    }

    private Recompensa crearRecompensa(
            Integer stock) {

        Recompensa recompensa =
            new Recompensa();

        recompensa.setId_recompensa(3);

        recompensa.setNombre(
            "Recompensa de prueba"
        );

        recompensa.setCosto_puntos(150);

        recompensa.setStock(stock);

        recompensa.setEstado(
            Recompensa.EstadoRecompensa.activo
        );

        return recompensa;
    }

    private Canje crearCanje(
            Canje.EstadoCanje estado,
            Usuario vecino,
            Recompensa recompensa,
            int puntosOriginales) {

        Canje canje = new Canje();

        canje.setId_canje(4);

        canje.setCodigo_ticket(
            CODIGO_TICKET
        );

        canje.setUsuario(vecino);

        canje.setRecompensa(recompensa);

        canje.setPuntos_descontados(
            puntosOriginales
        );

        canje.setEstado(
            estado
        );

        return canje;
    }

    private void simularAdministrador(
            Usuario administrador) {

        when(
            usuarioRepository.findByCorreo(
                CORREO_ADMIN
            )
        ).thenReturn(
            Optional.of(administrador)
        );
    }

    private void simularTicket(
            Canje canje) {

        when(
            canjeRepository.buscarPorCodigoTicket(
                CODIGO_TICKET
            )
        ).thenReturn(
            Optional.of(canje)
        );
    }

    private void verificarSinGuardados() {

        verify(
            usuarioRepository,
            never()
        ).save(any(Usuario.class));

        verify(
            recompensaRepository,
            never()
        ).save(any(Recompensa.class));

        verify(
            canjeRepository,
            never()
        ).save(any(Canje.class));
    }
}