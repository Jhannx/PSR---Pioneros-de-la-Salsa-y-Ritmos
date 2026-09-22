package com.jhanp.psraerobicsbackend.modules.aerobics.session.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    List<Session> findByDateOrderByStartTimeAsc(LocalDate date);

    /**
     * Rango de fechas: paginado, porque un rango amplio devuelve una sesión por día o más.
     * El filtro por fecha puntual no lo necesita — un día tiene una o dos.
     */
    Page<Session> findByDateBetweenOrderByDateDescStartTimeDesc(LocalDate from, LocalDate to, Pageable pageable);

    Page<Session> findAllByOrderByDateDescStartTimeDesc(Pageable pageable);

    Optional<Session> findByScheduleIdAndDate(Integer scheduleId, LocalDate date);

    /**
     * Para SessionCreationScheduler, que solo necesita saber si ya existe y no la fila entera.
     * <p>
     * Cuenta también las canceladas, y es intencionado: si un admin cancela una clase generada, esto
     * es lo que impide que el siguiente barrido se la vuelva a crear.
     */
    boolean existsByScheduleIdAndDate(Integer scheduleId, LocalDate date);

    /**
     * Sesión del día todavía en curso: sin hora de fin y no cancelada.
     */
    Optional<Session> findByDateAndEndTimeIsNullAndCancelledFalse(LocalDate date);

    List<Session> findByTeacherIdOrderByDateDesc(Integer teacherUserId);

    /**
     * Las de un rango filtrando por cancelada. Existe porque la pantalla de Sesiones pasa a mostrar
     * una semana con un filtro real de estado, en vez de un historico corrido con un interruptor de
     * "mostrar canceladas" que el cliente aplicaba despues de recibirlo todo.
     */
    Page<Session> findByDateBetweenAndCancelledOrderByDateDescStartTimeDesc(
            LocalDate from, LocalDate to, boolean cancelled, Pageable pageable);

    Page<Session> findByCancelledOrderByDateDescStartTimeDesc(boolean cancelled, Pageable pageable);

    /** Las del profesor en un rango, para su pantalla "Mis sesiones". */
    Page<Session> findByTeacherIdAndDateBetweenOrderByDateDescStartTimeDesc(
            Integer teacherUserId, LocalDate from, LocalDate to, Pageable pageable);

    Page<Session> findByTeacherIdOrderByDateDescStartTimeDesc(Integer teacherUserId, Pageable pageable);

    /**
     * Las sesiones de un horario a partir de una fecha, sin las canceladas. Se usa para reprogramar
     * las futuras cuando cambia la hora del horario.
     */
    List<Session> findByScheduleIdAndDateGreaterThanEqualAndCancelledFalse(Integer scheduleId, LocalDate from);

    /**
     * Las sesiones de hoy que acaban de empezar o de terminar dentro de la ventana dada.
     * <p>
     * Es lo que permite avisar de un cambio de estado que no provoco nadie: el de la sesion que
     * empieza o acaba por el reloj.
     * <p>
     * <b>JPQL y no nativa, y no es indiferente.</b> Session tiene cuatro campos {@code @Formula}
     * —status, teacherName y los dos contadores— que Hibernate resuelve anadiendolos al SELECT que
     * el mismo genera. En una consulta nativa no los anade: da por hecho que vienen en el ResultSet,
     * y un {@code SELECT *} no los trae. El sintoma es un error en tiempo de ejecucion que no dice
     * nada de formulas — "The column name status was not found in this ResultSet"— y solo aparece
     * cuando la consulta corre. Regla para este repositorio: <b>si devuelve Session, JPQL.</b>
     */
    @Query("""
            SELECT s FROM Session s
            WHERE s.date = :day
              AND s.cancelled = false
              AND ((s.startTime > :from AND s.startTime <= :to)
                OR (s.endTime   > :from AND s.endTime   <= :to))
            """)
    List<Session> findTransitionedBetween(@Param("day") LocalDate day,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);
}
