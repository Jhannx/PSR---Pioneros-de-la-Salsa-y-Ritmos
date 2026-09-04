package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Integer> {

    List<ScheduleException> findByScheduleIdOrderByDateAsc(Integer scheduleId);

    List<ScheduleException> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    boolean existsByScheduleIdAndDate(Integer scheduleId, LocalDate date);
}
