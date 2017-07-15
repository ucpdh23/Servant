package es.xan.servantv3;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import es.xan.servantv3.Scheduler.TimeExpression;

public class SchedulerTest {

	@SuppressWarnings("unused")
	public void scheduleWithRepetitionASecondAgo_tomorrow_ok() {
		LocalDateTime now = LocalDateTime.now();
		long nowEpoch = now.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
		
		LocalDateTime tomorrow = LocalTime.now().atDate(LocalDate.now().plusDays(1));
		long target = tomorrow.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
		
		TimeExpression at = Scheduler.at(LocalTime.now().plusSeconds(-1));
		long resolveDelay = at.resolveDelay();
		
	}

}
