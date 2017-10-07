// 
function initWidget(options) {
  if(options) {
    var $widget = $("#" + options.id + ".switch-widget");
    if(options.title) {
      $widget.find('.title').html(options.title);  
    }
    
    if(options.text) {
      $widget.find('.text').html(options.text);
    }
    
    if(options.expander) {
      $widget.find('.expander').html(options.expander);
    }
    
    if(options.switch_activeAction) {
      initToogle($widget, options.switch1_activeAction, options.switch1_inactiveAction);
    }
    if(options.button1_action) {
      initButton($widget.find('a.button1'), options.button1_action);
    }
    if(options.button2_action) {
      initButton($widget.find('a.button2'), options.button1_action);
    }
    if(options.button3_action) {
      initButton($widget.find('a.button3'), options.button1_action);
    }
    // Clase del icono fontAwesome
    if(options.iconClass) {
      $widget.find('.fa.icon').addClass(options.iconClass);  
    }
    
    // Opción para mostrar el switch habilitado o inhabilitado
    if(options.enable === false) {
      $widget.find('a.toggle').addClass('disabled');
      $widget.find('.fa.icon').addClass('disabled');
    } else {
      $widget.find('a.toggle').removeClass('disabled');
      $widget.find('.fa.icon').removeClass('disabled');
    }
    
    // Opción para mostrar el switch encendido o apagado. Apagado por defecto
    if(options.switch_active) {
      $widget.find('a.toggle').addClass('toggle-on')
      .removeClass('toggle-off')
      .removeClass('toggle-moving');
      $widget.find('.fa.icon').addClass('active');
    } else {
      $widget.find('a.toggle').removeClass('toggle-on')
      .addClass('toggle-off')
      .removeClass('toggle-moving');
      $widget.find('.fa.icon').removeClass('active');
    }
  }
}

function initButton($button, action) {
  $button.click(function() {
    e.preventDefault();
    action();
  });
}


var w = window,
    d = document,
    e = d.documentElement,
    g = d.getElementsByTagName('body')[0],
    x = w.innerWidth || e.clientWidth || g.clientWidth,
    y = w.innerHeight|| e.clientHeight|| g.clientHeight;

$("#screen").html(x+'x'+y);

function initToogle($widget, onActive, onInactive) {
  $widget.find('a.toggle').click(function(e) {
    var toggle = this;
    e.preventDefault();
    if(!$(toggle).hasClass('disabled')) {
      $(toggle).toggleClass('toggle-on')
        .toggleClass('toggle-off')
        .addClass('toggle-moving');
      var icon = $(toggle).parents('.switch-widget:first').find('.fa.icon');
      if($(toggle).hasClass('toggle-on')) {
          $(icon).addClass('active');
          onActive(toggle, icon, $widget);
        } else {
          $(icon).removeClass('active');
          onInactive(toggle, icon, $widget);
        }
      setTimeout(function() {
        $(toggle).removeClass('toggle-moving');
      }, 200);
    }
  });
}