package es.xan.servantv3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

	private Vertx mVertx;
	private final Map<UUID, Long> mContainer;

	public Scheduler(Vertx vertx) {
		Validate.notNull(vertx, "vertx cannot be null");
		
		this.mVertx = vertx;
		this.mContainer = new HashMap<>();
	}
	
	public UUID scheduleTask(TimeExpression expression, Function<UUID, Boolean> function) {
		Validate.notNull(expression, "expression cannot be null");
		Validate.notNull(function, "function cannot be null");
		
		UUID uuid = UUID.randomUUID();
		this.mContainer.put(uuid, -1L);
		
		scheduleTask(uuid, expression, function);
		
		return uuid;
	}
	
	public void removeScheduledTask(UUID uuid) {
		Validate.notNull(uuid, "uuid cannot be null");

		Long vertxId = this.mContainer.get(uuid);
		this.mVertx.cancelTimer(vertxId);
	}
	
	private void scheduleTask(UUID uuid, TimeExpression expression, Function<UUID, Boolean> function) {
		long vertxId = this.mVertx.setTimer(expression.resolveDelay(), (event) -> {
				Boolean withReply = function.apply(uuid);
				
				if (expression.isPeriodic() && withReply) {
					scheduleTask(uuid, expression, function);
				}
		});
		
		this.mContainer.put(uuid, vertxId);
	}
	
	public static TimeExpression in(int value, ChronoUnit unit) {
		Validate.notNull(value, "value cannot be null");
		Validate.notNull(unit, "unit cannot be null");
		
		return new TimeExpression(LocalDateTime.now().plus(value, unit));
	}
	
	public static TimeExpression at(LocalTime lt) {
		Validate.notNull(lt, "lt cannot be null");
		
		return new TimeExpression(lt);
	}
	
	public static TimeExpression at(LocalDateTime lt) {
		Validate.notNull(lt, "lt cannot be null");
		
		return new TimeExpression(lt);
	}
	
	public static class TimeExpression {
		private LocalDateTime currentDateTimeTarget;
		private LocalTime currentTimeTarget;
		
		private final boolean mIsPeriodic;
		
		TimeExpression(LocalTime target) {
			this.currentTimeTarget = target;
			this.mIsPeriodic = true;
		}
		
		TimeExpression(LocalDateTime target) {
			this.currentDateTimeTarget = target;
			this.mIsPeriodic = false;
		}
		

		protected long resolveDelay() {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				LOGGER.warn(e.getMessage(), e);
			}
			
			LocalDateTime now = LocalDateTime.now();
			
			LocalDateTime target;
			if (currentDateTimeTarget != null) {
				target = currentDateTimeTarget;
			} else {
				if (now.toLocalTime().isAfter(currentTimeTarget)) {
					// For tomorrow
					target = currentTimeTarget.atDate(LocalDate.now().plusDays(1));
				} else {
					// For today
					target = currentTimeTarget.atDate(LocalDate.now());
				}
			}
			
			if (now.isAfter(target))  throw new RuntimeException("invalid time expression " + target);
			
			ZoneId zoneId = ZoneId.systemDefault(); 
			long nowEpoch = now.atZone(zoneId).toEpochSecond();
			long targetEpoch = target.atZone(zoneId).toEpochSecond();
			
			return (targetEpoch - nowEpoch) * 1000;
		}
		
		protected boolean isPeriodic() {
			return this.mIsPeriodic;
		}
		
		
	}
}
