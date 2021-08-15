package com.twendee.fpl.service;

import com.twendee.fpl.constant.Constant;
import com.twendee.fpl.dto.*;
import com.twendee.fpl.model.GameWeekResult;
import com.twendee.fpl.model.Team;
import com.twendee.fpl.repository.GameWeekResultRepository;
import com.twendee.fpl.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MainService {

    @Autowired
    TeamRepository teamRepository;
    @Autowired
    GameWeekResultRepository gameWeekResultRepository;

    public FullGameWeekResultDTO getFullGameWeekResult(Integer gameWeek){
        FullGameWeekResultDTO dto = new FullGameWeekResultDTO();
        List<GameWeekResult> gameWeekResults = gameWeekResultRepository.findByGameWeek(gameWeek);
        List<GameWeekResultDTO> gameWeekResultDTOS = gameWeekResults.stream().map(GameWeekResultDTO::new).sorted(Comparator.comparing(GameWeekResultDTO::getPosition)).collect(Collectors.toList());
        dto.getGameWeekResultDTOList().addAll(gameWeekResultDTOS);

        List<Long> doneList = new ArrayList<>();
        List<PairH2HDTO> h2HDTOList = new ArrayList<>();
        gameWeekResults.forEach(g -> {
            if (!doneList.contains(g.getTeam().getFplId())) {
                PairH2HDTO pairH2HDTO = new PairH2HDTO();
                pairH2HDTO.setTeam1Name(g.getTeam().getFplName());
                pairH2HDTO.setTeam1Point(g.getPoint());
                doneList.add(g.getTeam().getFplId());

                GameWeekResult rival = gameWeekResults.stream().filter(gameWeekResult -> gameWeekResult.getTeam().getFplId().equals(g.getRival().getFplId())).findAny().orElse(null);
                if (rival != null){
                    pairH2HDTO.setTeam2Name(rival.getTeam().getFplName());
                    pairH2HDTO.setTeam2Point(rival.getPoint());
                    doneList.add(rival.getTeam().getFplId());
                }
                h2HDTOList.add(pairH2HDTO);
            }
        });

        dto.getH2HDTOList().addAll(h2HDTOList);

        return dto;
    }

    public String addTeam(ListTeamDTO dto){
        List<Team> listToCreate = new ArrayList<>();
        try {
            dto.getList().forEach(t -> {
                Team team = new Team();
                team.setName(t.getName());
                team.setFplId(t.getFplId());
                team.setFplName(t.getFplName());

                listToCreate.add(team);
            });
            teamRepository.saveAll(listToCreate);
            return "SUCCESS";
        }
        catch (Exception e){
            return "FAIL";
        }
    }

    public String updateFixture(ListListH2HDTO dtos){
        List<GameWeekResult> listToCreate = new ArrayList<>();
        try{
            dtos.getList().forEach(dto -> {
                dto.getList().forEach(p -> {
                    Team team1 = teamRepository.findByFplId(p.getTeam1());
                    Team team2 = teamRepository.findByFplId(p.getTeam2());

                    GameWeekResult gameWeekResult1 = new GameWeekResult();
                    gameWeekResult1.setGameWeek(dto.getGameWeek());
                    gameWeekResult1.setTeam(team1);
                    gameWeekResult1.setRival(team2);
                    listToCreate.add(gameWeekResult1);
//                    gameWeekResultRepository.save(gameWeekResult1);

                    GameWeekResult gameWeekResult2 = new GameWeekResult();
                    gameWeekResult2.setGameWeek(dto.getGameWeek());
                    gameWeekResult2.setTeam(team2);
                    gameWeekResult2.setRival(team1);
                    listToCreate.add(gameWeekResult2);
//                    gameWeekResultRepository.save(gameWeekResult2);
                });
            });
            gameWeekResultRepository.saveAll(listToCreate);
            return "SUCCESS - created data for " + listToCreate.size() + " game week";
        }
        catch (Exception e)
        {
            return "FAIL";
        }
    }

    public GameWeekResultDTO updateGameWeekResult(GameWeekPointDTO dto)
    {
        List<GameWeekResult> previousGameWeekResults = new ArrayList<>();
        if (dto.getGameWeek() > 1){
            previousGameWeekResults.addAll(gameWeekResultRepository.findByGameWeekOrderByPositionAsc(dto.getGameWeek()-1));
        }

//        Long top = previousGameWeekResults.stream().filter(g -> g.getPosition() == 1).map(g -> g.getTeam().getFplId()).findAny().orElse(0L);
//        Long bottom = previousGameWeekResults.stream().filter(g -> g.getPosition() == previousGameWeekResults.size()).map(g -> g.getTeam().getFplId()).findAny().orElse(0L);

        Long top = previousGameWeekResults.get(0).getTeam().getFplId();
        Long bottom = previousGameWeekResults.get(previousGameWeekResults.size() - 1).getTeam().getFplId();

        List<Long> topAndBottom = Arrays.asList(top, bottom);

        List<GameWeekResult> gameWeekResults = gameWeekResultRepository.findByGameWeek(dto.getGameWeek());
        GameWeekResult currentGameWeekResult = gameWeekResults.stream().filter(g -> g.getTeam().getFplId().equals(dto.getTeamId())).findAny().orElse(null);
        GameWeekResult rivalResult = gameWeekResults.stream().filter(g -> {
            assert currentGameWeekResult != null;
            if (currentGameWeekResult.getRival() != null) {
                return g.getTeam().getFplId().equals(currentGameWeekResult.getRival().getFplId());
            }
            else {
                return false;
            }
        }).findAny().orElse(null);
        int currentIndex = gameWeekResults.indexOf(currentGameWeekResult);

        gameWeekResults.get(currentIndex).setTransfer(dto.getTransfer());
        gameWeekResults.get(currentIndex).setMinusPoints(dto.getMinusPoints());
        gameWeekResults.get(currentIndex).setPoint(calculateRealPoint(dto.getPoint(), dto.getMinusPoints()));
        gameWeekResults.get(currentIndex).setLocalPoint(getBonusPointFromPreviousGameWeek(topAndBottom, currentGameWeekResult.getTeam().getFplId(), dto.getPoint(), dto.getMinusPoints()));

        GameWeekResultDTO gameWeekResultDTO = new GameWeekResultDTO(gameWeekResults.get(currentIndex));

        gameWeekResults.sort(Comparator.comparing(GameWeekResult::getLocalPoint).reversed());
        int order = 1;
        for (GameWeekResult result : gameWeekResults) {
            result.setPosition(order);
            if (order == 1){
                result.setMoney(Constant.FIRST);
            }
            else if (order == 2 || order == 3){
                result.setMoney(0D);
            }
            else if (order == 4){
                result.setMoney(Constant.FOURTH);
            }
            else if (order == 5){
                result.setMoney(Constant.FIFTH);
            }
            else if (order == 6){
                result.setMoney(Constant.SIXTH);
            }

            if (rivalResult != null && result.getTeam().getFplId().equals(currentGameWeekResult.getTeam().getFplId())){
                result.setMoney(result.getMoney() + Constant.LOOSE_H2H);
            }

            order++;
        }
        gameWeekResultRepository.saveAll(gameWeekResults);

        updateMainTable(dto.getGameWeek());

        return gameWeekResultDTO;
    }

    private Integer getBonusPointFromPreviousGameWeek(List<Long> topAndBottom, Long current, Integer point, Double minusPoints){
        if (minusPoints != 0){
            if (topAndBottom.contains(current)){
                return point - minusPoints.intValue() + 4;
            }
        }
        return point - minusPoints.intValue();
    }

    private void updateMainTable(Integer gameWeek){
        List<Team> teams = teamRepository.findAllByOrderByPositionAsc();
        teams.forEach(team -> {
            List<GameWeekResult> gameWeekResults = gameWeekResultRepository.findByTeamAndGameWeekLessThan(team, gameWeek+1);
            int sumPoint = gameWeekResults.stream().mapToInt(GameWeekResult::getPoint).sum();
            double sumMoney = gameWeekResults.stream().mapToDouble(GameWeekResult::getMoney).sum();
            team.setPoint(sumPoint);
            team.setMoney(sumMoney);
        });

        teams.sort(Comparator.comparing(Team::getPoint).reversed());
        int order = 1;
        for (Team team : teams) {
            team.setPosition(order);
            order++;
        }
        teamRepository.saveAll(teams);
    }

    private Integer calculateRealPoint(Integer point, Double minusPoints)
    {
        return point - minusPoints.intValue();
    }
}
