package es.xan.servantv3.weather;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.messages.Query;
import es.xan.servantv3.messages.Temperature;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WeatherVerticle extends AbstractServantVerticle  {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherVerticle.class);

    public WeatherVerticle() {
        super(Constant.WHEATHER_VERTICLE);

        supportedActions(WeatherVerticle.Actions.values());
    }

    public enum Actions implements Action {
        RESOLVE_FORECAST(null);

        private Class<?> mBeanClass;

        private Actions (Class<?> beanClass) {
            this.mBeanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return this.mBeanClass;
        }
    }

    public void resolve_forecast(Message<Object> message) {
        WeatherUtils.resolveHourlyInfo();
    }

}
