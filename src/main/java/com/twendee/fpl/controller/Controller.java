package com.twendee.fpl.controller;

import com.twendee.fpl.dto.*;
import com.twendee.fpl.model.GameWeekResult;
import com.twendee.fpl.model.Team;
import com.twendee.fpl.repository.GameWeekResultRepository;
import com.twendee.fpl.repository.TeamRepository;
import com.twendee.fpl.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class Controller {

    @Autowired
    TeamRepository teamRepository;
    @Autowired
    GameWeekResultRepository gameWeekResultRepository;
    @Autowired
    MainService mainService;

    @GetMapping("/league-table")
    public ResponseEntity<List<Team>> getAll() {
        List<Team> teams = teamRepository.findAllByOrderByPositionAsc();
        if (teams.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<Team>>(teams, HttpStatus.OK);
    }

    @PostMapping("/add-team")
    public ResponseEntity<String> addTeam(@RequestBody ListTeamDTO dto) {
        String result = mainService.addTeam(dto);
        if (result.equals("FAIL")){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<String>(result, HttpStatus.CREATED);
    }

    @GetMapping("/get-team-info/{id}")
    public ResponseEntity<Team> getOne(@PathVariable(name = "id") Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<Team>(team, HttpStatus.OK);
    }

    @GetMapping("/get-game-week-info-by-team")
    public ResponseEntity<GameWeekResultDTO> getGameWeekInfoByTeam(@RequestParam(name = "teamId") Long teamId,
                                                                   @RequestParam(name = "gameWeek") Integer gameWeek) {
        Team team = teamRepository.findById(teamId).orElse(null);
        GameWeekResult gameWeekResult = gameWeekResultRepository.findByGameWeekAndTeam(gameWeek, team);
        if (gameWeekResult == null) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<GameWeekResultDTO>(new GameWeekResultDTO(gameWeekResult), HttpStatus.OK);
    }

    @GetMapping("/get-game-week-info")
    public ResponseEntity<List<GameWeekResultDTO>> getGameWeekInfo(@RequestParam(name = "gameWeek") Integer gameWeek) {
        List<GameWeekResult> gameWeekResults = gameWeekResultRepository.findByGameWeek(gameWeek);
        if (CollectionUtils.isEmpty(gameWeekResults)) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }

        List<GameWeekResultDTO> gameWeekResultDTOS = gameWeekResults.stream().map(GameWeekResultDTO::new).sorted(Comparator.comparing(GameWeekResultDTO::getPosition)).collect(Collectors.toList());
        return new ResponseEntity<List<GameWeekResultDTO>>(gameWeekResultDTOS, HttpStatus.OK);
    }

    @PostMapping("/update-league-result")
    public ResponseEntity<GameWeekResultDTO> updateFromH2HResult(@RequestBody GameWeekPointDTO dto) {
        GameWeekResultDTO gameWeekResultDTO = mainService.updateGameWeekResult(dto);
        return new ResponseEntity<GameWeekResultDTO>(gameWeekResultDTO, HttpStatus.OK);
    }

    @PostMapping("/update-fixture")
    public ResponseEntity<String> updateFixture(@RequestBody ListListH2HDTO dtos) {
        String result = mainService.updateFixture(dtos);
        return new ResponseEntity<String>(result, HttpStatus.OK);
    }


}
