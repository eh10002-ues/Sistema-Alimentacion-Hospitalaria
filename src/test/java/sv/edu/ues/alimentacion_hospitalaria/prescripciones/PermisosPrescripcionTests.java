package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence.PrescripcionDietaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** Matriz de permisos HTTP de las rutas agregadas por SAH-2, con los controladores reales. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PermisosPrescripcionTests {

    private static final Set<Rol> PRESCRIPCION = Set.of(Rol.ROLE_MEDICO, Rol.ROLE_NUTRICIONISTA);
    private static final Set<Rol> COCINA = Set.of(Rol.ROLE_COCINA);

    @Autowired private MockMvc mvc;
    @Autowired private PrescripcionDietaRepository prescripciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        var datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        Arrays.stream(Rol.values()).forEach(rol -> datos.usuario(nombre(rol), rol));
        paciente = datos.pacienteIngresado();
    }

    @ParameterizedTest(name = "{0}: {1} {2} permitido={3}")
    @MethodSource("matriz")
    void aplicaLaMatrizDeSah2(Rol rol, String metodo, String ruta, boolean permitido) throws Exception {
        String rutaReal = ruta.replace("{id}", String.valueOf(paciente.getId()));
        var peticion = MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), rutaReal).param("regimen", "BLANDA")
            .with(user(nombre(rol)).authorities(new SimpleGrantedAuthority(rol.name()))).with(csrf());
        var resultado = mvc.perform(peticion);
        if (permitido) {
            resultado.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        } else {
            resultado.andExpect(status().isForbidden()).andExpect(forwardedUrl("/acceso-denegado"));
        }
        if (!(permitido && metodo.equals("POST"))) {
            assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
        }
    }

    static Stream<Arguments> matriz() {
        record Caso(String ruta, Set<Rol> lectura, Set<Rol> post) { }
        var casos = Stream.of(
            // POST a /prescripciones lo permite la cadena de seguridad aunque no tenga manejador (responde 405).
            new Caso("/prescripciones", PRESCRIPCION, PRESCRIPCION),
            new Caso("/prescripciones/pacientes/{id}", PRESCRIPCION, PRESCRIPCION),
            new Caso("/cocina/ordenes", COCINA, Set.of())).toList();
        return Arrays.stream(Rol.values()).flatMap(rol -> casos.stream().flatMap(caso ->
            Stream.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE").map(metodo -> {
                boolean lectura = metodo.equals("GET") || metodo.equals("HEAD");
                boolean permitido = lectura ? caso.lectura.contains(rol)
                    : metodo.equals("POST") && caso.post.contains(rol);
                return Arguments.of(rol, metodo, caso.ruta, permitido);
            })));
    }

    @Test
    void soloCocinaAbreElCanalDeEventos() throws Exception {
        for (Rol rol : Rol.values()) {
            var resultado = mvc.perform(get("/cocina/ordenes/eventos")
                .with(user(nombre(rol)).authorities(new SimpleGrantedAuthority(rol.name()))));
            if (rol == Rol.ROLE_COCINA) {
                resultado.andExpect(request().asyncStarted());
            } else {
                resultado.andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/cocina/ordenes/eventos")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/prescripciones")).andExpect(redirectedUrl("/login"));
    }

    private static String nombre(Rol rol) {
        return "permiso." + rol.name().toLowerCase().replace("role_", "").replace('_', '.');
    }
}
