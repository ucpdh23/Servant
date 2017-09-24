$('document').ready(function() {  
  initWidgets();

  initWebSocket();
});


function initWidgets() {

// GENERAL

  // Exterior

  initWidget({
    id: "widget-outside",
    title: "Exterior",
    text: "-",
    enable: false,
    switch_active: false,
    iconClass: "fa-thermometer-half fa-5x" 
  });

  // livingRoom

  initWidget({
    id: "widget-livingRoom",
    title: "Salon",
    text: "-",
    enable: false,
    switch_active: false,
    iconClass: "fa-thermometer-three-quarters fa-5x" 
  });

  // bedRoom

  initWidget({
    id: "widget-bedRoom",
    title: "Habitacion",
    text: "-",
    enable: false,
    switch_active: false,
    iconClass: "fa-thermometer-three-quarters fa-5x" 
  });
  
  
}

/**
 * Init web socket connection
 */
var eb;

function initWebSocket() {
	eb = new EventBus("./eventbus");
  	eb.onopen = function () {
    	eb.registerHandler("event", function (err, msg) {
    		processMessage(msg.body);
    	});
    
    	eb.send('temperature.verticle', {action: 'LAST_VALUES'}, function (err, msg) {
    		console.log("reply:" + JSON.stringify(msg));
    		msg.body.result.forEach(temperature => 
    			initWidget({
      				id: "widget-" + temperature.room,
      				enable: true,
      				text: temperature.temperature
    			})
    		);
    	});
    };
 }


/**
 * Required messages to fill widgets info.
 */

function processMessage(message) {
  console.log("Message: " + JSON.stringify(message));

  // Parse string message to object
  var bean = message.bean;
  
  switch(message.action) {    
  
  case 'TEMPERATURE_RECEIVED':
    initWidget({
      id: "widget-" + bean.room,
      enable: true,
      text: bean.temperature,
      expander: new Date(bean.timestamp)
    });
    break;

  default:
    console.log('No actions defined to process ' + message);
  }
}

function expander(item) {
    $(item).toggleClass('switch-widget-expanded');
    $(item).toggleClass('switch-widget-condensed');
    
    $(item).find('div.switch-widget-expansion').toggle();
}