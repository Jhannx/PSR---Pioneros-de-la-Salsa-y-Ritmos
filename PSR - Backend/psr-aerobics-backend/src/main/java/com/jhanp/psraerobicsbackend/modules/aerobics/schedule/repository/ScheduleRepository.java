package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    List<Schedule> findByActiveTrueOrderByDayOfWeekAscStartTimeAsc();

    List<Schedule> findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(Integer dayOfWeek);

    List<Schedule> findAllByOrderByDayOfWeekAscStartTimeAsc();
}
