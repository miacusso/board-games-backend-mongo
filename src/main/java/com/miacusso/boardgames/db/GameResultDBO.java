package com.miacusso.boardgames.db;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameResultDBO {

	private Integer id;
	private LocalDate date;
	private Integer winner;
	private Integer game;

}
