package sv.edu.ues.alimentacion_hospitalaria.insumos.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaDemandaProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaProyeccionInsumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.PeriodoProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.ProyeccionInsumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.ProyeccionInsumos;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.ProyeccionInsumosRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA', 'COCINA')")
public class ProyeccionInsumosService {

    private static final BigDecimal UNO = BigDecimal.ONE;
    private static final MathContext CONTEXTO = MathContext.DECIMAL128;
    private static final String ADVERTENCIA_MERMA =
        "Merma no registrada; se consideró 0%.";
    private static final String ADVERTENCIA_PRESENTACION =
        "Configure una cantidad de presentación mayor que cero para calcular unidades de compra.";

    private final ProyeccionInsumosRepository proyeccionRepository;
    private final RegistroBitacoraService registroBitacoraService;
    private final UsuarioRepository usuarioRepository;

    public ProyeccionInsumosService(ProyeccionInsumosRepository proyeccionRepository,
                                    RegistroBitacoraService registroBitacoraService,
                                    UsuarioRepository usuarioRepository) {
        this.proyeccionRepository = proyeccionRepository;
        this.registroBitacoraService = registroBitacoraService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ProyeccionInsumos proyectar(PeriodoProyeccion periodo, BigDecimal margenFraccion,
                                       LocalDate fechaInicio) {
        Objects.requireNonNull(periodo, "El período de proyección es obligatorio.");
        Objects.requireNonNull(margenFraccion, "El margen es obligatorio.");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio es obligatoria.");
        if (margenFraccion.signum() < 0 || margenFraccion.compareTo(UNO) > 0) {
            throw new IllegalArgumentException("El margen debe ser una fracción entre 0 y 1.");
        }

        LocalDate fechaFin = fechaInicio.plusDays(periodo.getDias() - 1L);
        BigDecimal margen = margenFraccion;
        List<FilaProyeccionInsumo> filas = proyeccionRepository.buscarFilasVigentes(
            fechaInicio, fechaFin, EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO);
        List<FilaDemandaProyeccion> demanda = proyeccionRepository.buscarDemandaVigente(
            fechaInicio, fechaFin, EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO);

        Map<Long, Acumulado> acumulados = new LinkedHashMap<>();
        for (FilaProyeccionInsumo fila : filas) {
            Acumulado acumulado = acumulados.computeIfAbsent(fila.insumoId(), id -> new Acumulado(fila));
            LocalDate desde = fila.fechaInicio().isAfter(fechaInicio) ? fila.fechaInicio() : fechaInicio;
            LocalDate hasta = fila.fechaFin() == null || fila.fechaFin().isAfter(fechaFin)
                ? fechaFin : fila.fechaFin();
            long diasVigentes = hasta.toEpochDay() - desde.toEpochDay() + 1;
            if (diasVigentes > 0) {
                BigDecimal cantidad = Objects.requireNonNull(
                    fila.cantidadPorRacion(), "La cantidad por ración del insumo no puede ser nula.");
                if (cantidad.signum() < 0) {
                    throw new IllegalStateException("La cantidad por ración no puede ser negativa.");
                }
                acumulado.neto = acumulado.neto.add(cantidad.multiply(BigDecimal.valueOf(diasVigentes)));
            }
        }

        List<ProyeccionInsumo> insumos = acumulados.values().stream()
            .map(acumulado -> acumularResultado(acumulado, margen))
            .toList();
        Set<Long> pacientes = new LinkedHashSet<>();
        long racionesEnPeriodo = 0;
        for (FilaDemandaProyeccion fila : demanda) {
            pacientes.add(fila.pacienteId());
            LocalDate desde = fila.fechaInicio().isAfter(fechaInicio) ? fila.fechaInicio() : fechaInicio;
            LocalDate hasta = fila.fechaFin() == null || fila.fechaFin().isAfter(fechaFin)
                ? fechaFin : fila.fechaFin();
            long diasVigentes = hasta.toEpochDay() - desde.toEpochDay() + 1;
            if (diasVigentes > 0) {
                racionesEnPeriodo += diasVigentes;
            }
        }
        BigDecimal racionesPorDia = BigDecimal.valueOf(racionesEnPeriodo)
            .divide(BigDecimal.valueOf(periodo.getDias()), CONTEXTO);
        List<String> advertencias = new ArrayList<>();
        insumos.forEach(insumo -> advertencias.addAll(insumo.advertencias()));
        boolean costoCompleto = insumos.stream().allMatch(insumo -> insumo.costo() != null);
        if (!costoCompleto) {
            advertencias.add("Falta registrar costo unitario en uno o más insumos; el presupuesto total no está disponible.");
        }
        BigDecimal costoTotal = costoCompleto
            ? insumos.stream().map(ProyeccionInsumo::costo).reduce(BigDecimal.ZERO, BigDecimal::add)
            : null;
        registroBitacoraService.registrar("PROYECTAR", "insumos", null, usuarioActual().getId());
        return new ProyeccionInsumos(
            periodo,
            fechaInicio,
            fechaFin,
            margenFraccion.multiply(new BigDecimal("100")),
            insumos,
            costoTotal,
            pacientes.size(),
            racionesPorDia,
            List.copyOf(new LinkedHashSet<>(advertencias)));
    }

    private Usuario usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
        if (!usuario.isActivo() || !puedeProyectar(usuario.getRol())) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return usuario;
    }

    private boolean puedeProyectar(Rol rol) {
        return rol == Rol.ROLE_ADMIN || rol == Rol.ROLE_NUTRICIONISTA || rol == Rol.ROLE_COCINA;
    }

    private ProyeccionInsumo acumularResultado(Acumulado acumulado, BigDecimal margen) {
        BigDecimal merma = acumulado.fila.merma();
        if (merma == null) {
            merma = BigDecimal.ZERO;
            acumulado.advertencias.add(ADVERTENCIA_MERMA);
        } else if (merma.signum() < 0 || merma.compareTo(UNO) >= 0) {
            throw new IllegalStateException("La merma del insumo debe ser una fracción entre 0 y 1.");
        }

        BigDecimal factorMerma = UNO.subtract(merma);
        BigDecimal factorMargen = UNO.add(margen);
        BigDecimal bruto = acumulado.neto.divide(factorMerma, CONTEXTO);
        BigDecimal totalConMargen = acumulado.neto.multiply(factorMargen)
            .divide(factorMerma, CONTEXTO);
        BigDecimal cantidadPresentacion = acumulado.fila.cantidadPresentacion();
        BigDecimal cantidadComprar = null;
        if (cantidadPresentacion != null && cantidadPresentacion.signum() > 0) {
            BigDecimal numeradorCompra = acumulado.neto.multiply(factorMargen);
            BigDecimal denominadorCompra = factorMerma.multiply(cantidadPresentacion);
            cantidadComprar = numeradorCompra.divide(denominadorCompra, 0, RoundingMode.CEILING);
        } else {
            acumulado.advertencias.add(ADVERTENCIA_PRESENTACION);
        }
        BigDecimal costo = acumulado.fila.costoUnitario() == null
            ? null : totalConMargen.multiply(acumulado.fila.costoUnitario());
        if (costo == null) {
            acumulado.advertencias.add(
                "Costo unitario no registrado; no se puede calcular el costo de este insumo.");
        }

        return new ProyeccionInsumo(
            acumulado.fila.insumoId(),
            acumulado.fila.nombre(),
            acumulado.fila.unidad(),
            acumulado.neto,
            bruto,
            totalConMargen,
            acumulado.fila.presentacion(),
            cantidadPresentacion,
            cantidadComprar,
            costo,
            List.copyOf(acumulado.advertencias));
    }

    private static final class Acumulado {
        private final FilaProyeccionInsumo fila;
        private final LinkedHashSet<String> advertencias = new LinkedHashSet<>();
        private BigDecimal neto = BigDecimal.ZERO;

        private Acumulado(FilaProyeccionInsumo fila) {
            this.fila = fila;
        }
    }
}
