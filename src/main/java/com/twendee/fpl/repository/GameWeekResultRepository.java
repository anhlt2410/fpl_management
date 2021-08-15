package com.twendee.fpl.repository;

import com.twendee.fpl.model.GameWeekResult;
import com.twendee.fpl.repository.custom.GameWeekRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameWeekResultRepository extends JpaRepository<GameWeekResult, Long>, GameWeekRepositoryCustom {
//    Optional<GameWeekResult> findByGameWeekAndTeamId(Integer gameWeek, Integer teamId);

    List<GameWeekResult> findByGameWeek(int gameWeek);

    List<GameWeekResult> findByGameWeekOrderByPositionAsc(int gameWeek);
}
