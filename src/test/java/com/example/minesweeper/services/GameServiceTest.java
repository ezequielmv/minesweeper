package com.example.minesweeper.services;

import com.example.minesweeper.model.Action;
import com.example.minesweeper.model.Cell;
import com.example.minesweeper.model.CellState;
import com.example.minesweeper.model.Game;
import com.example.minesweeper.model.GameState;
import com.example.minesweeper.model.MatrixRow;
import com.example.minesweeper.model.PlayMoveDTO;
import com.example.minesweeper.model.StartGameDTO;
import com.example.minesweeper.repositories.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    GameRepository gameRepository;

    @InjectMocks
    GameService gameService;

    static final int ROW_SIZE = 3;
    static final int COLUMN_SIZE = 3;
    static final String USER_NAME = "test user name";
    static final int MINE_PERCENTAGE = 50;
    static final int MINE_QUANTITY = 4;
    static final int EXPECTED_CELL_QUANTITY = ROW_SIZE * COLUMN_SIZE;
    static final int EXPECTED_UNOPENED_QUANTITY = EXPECTED_CELL_QUANTITY;


    @Test
    void createAndInitializeGame() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, MINE_PERCENTAGE);
        when(gameRepository.save(any())).thenReturn(customGame);

        var startGameDTO = new StartGameDTO(USER_NAME, ROW_SIZE, COLUMN_SIZE, MINE_PERCENTAGE);

        var game = this.gameService.createGame(startGameDTO);
        var boardCells = game.getBoard().getRows().stream().map(MatrixRow::getCells).flatMap(Collection::stream).collect(Collectors.toList());

        var cellQuantity = boardCells.size();

        var mineQuantity = boardCells.stream().filter(Cell::isMined).count();
        var unopenedQuantity = boardCells.stream().filter(cell -> cell.getState().equals(CellState.UNOPENED)).count();

        // number of cells
        assertEquals(EXPECTED_CELL_QUANTITY, cellQuantity);

        assertEquals(COLUMN_SIZE, game.getBoard().getColumnSize());
        assertEquals(ROW_SIZE, game.getBoard().getRowSize());
        assertEquals(USER_NAME, game.getUserName());
        assertEquals(MINE_QUANTITY, mineQuantity);
        assertEquals(EXPECTED_UNOPENED_QUANTITY, unopenedQuantity);


        verify(gameRepository, times(1)).save(any());
    }

    @Test
    void playOpenCell() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, MINE_PERCENTAGE);
        var playMoveDTO = new PlayMoveDTO(Action.OPEN, 1, 1);
        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        // force mined false on this cell only for testing purposes
        customGame.getBoard().getRows().get(1).getCells().get(1).setMined(false);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        assertEquals(GameState.INPROGRESS, updatedGame.getState());
        assertEquals(CellState.OPENED, updatedGame.getBoard().getRows().get(1).getCells().get(1).getState());
    }

    @Test
    void playOpenCellMined() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 100);
        var playMoveDTO = new PlayMoveDTO(Action.OPEN, 1, 1);

        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        assertEquals(GameState.FINISHED, updatedGame.getState());
        assertEquals(CellState.OPENED, updatedGame.getBoard().getRows().get(1).getCells().get(1).getState());
    }

    @Test
    void playFlagCell() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 100);
        var playMoveDTO = new PlayMoveDTO(Action.FLAG, 1, 1);
        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        assertEquals(GameState.INPROGRESS, updatedGame.getState());
        assertEquals(CellState.FLAGGED, updatedGame.getBoard().getRows().get(1).getCells().get(1).getState());
    }

    @Test
    void playQuestionMarkCell() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 100);
        var playMoveDTO = new PlayMoveDTO(Action.QUESTION_MARK, 1, 1);
        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        assertEquals(GameState.INPROGRESS, updatedGame.getState());
        assertEquals(CellState.QUESTION_MARK, updatedGame.getBoard().getRows().get(1).getCells().get(1).getState());
    }

    @Test
    void playClearCell() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 100);
        var playMoveDTO = new PlayMoveDTO(Action.CLEAR, 1, 1);

        customGame.getBoard().getRows().get(0).getCells().get(0).setState(CellState.FLAGGED);

        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        assertEquals(GameState.INPROGRESS, updatedGame.getState());
        assertEquals(CellState.UNOPENED, updatedGame.getBoard().getRows().get(1).getCells().get(1).getState());
    }

    @Test
    void playOpenCellWithNoMinesShouldOpenAll() {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 0);
        var playMoveDTO = new PlayMoveDTO(Action.OPEN, 0, 0);

        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        var updatedGame = this.gameService.play(1L, playMoveDTO);
        var countOfCellsUnopened = updatedGame.getBoard().getRows().stream().map(MatrixRow::getCells).flatMap(Collection::stream).filter(it -> it.getState() != CellState.OPENED).count();

        assertEquals(GameState.FINISHED, updatedGame.getState());
        assertEquals(0L, countOfCellsUnopened);

    }

    @Test
    void pauseGame() throws InterruptedException {
        var customGame = new Game(USER_NAME, ROW_SIZE, COLUMN_SIZE, 0);

        when(gameRepository.findById(any())).thenReturn(java.util.Optional.of(customGame));
        when(gameRepository.save(any())).thenReturn(customGame);

        TimeUnit.SECONDS.sleep(2);
        var updatedGame = this.gameService.pause(1L);
        var twoSecondsInMilliseconds = Duration.ofSeconds(2).toMillis();

        assertTrue(updatedGame.getTimeElapsed() > twoSecondsInMilliseconds);
        assertEquals(GameState.PAUSED, updatedGame.getState());
    }


}
