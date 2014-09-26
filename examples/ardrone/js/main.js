$(document).ready(function(){
  if (typeof xwalk.experimental.ardrone === "undefined") {
    document.title = "Fail";
    alert("Ardrone not supported!!!");
    console.log("Ardrone not supported!!!");
    return;
  }

  var droneVideo = xwalk.experimental.ardrone.video || null;
  if (!droneVideo) {
    console.log('xwalk.experimental.ardrone.video is not avilable.');
    return;
  }

  // Customize your ARDrone ip address here.
  option = new ARDroneVideoOption();
  option.ipAddress = "192.168.1.1";

  droneVideo.play('ardrone_video', option)
    .then(function() {
      console.log('Play okay');
    }, function(e) {
      console.log(e.message);
    });

  var width = $(window).width();
  var height = $(window).height();

  $("body").css("transform", "scale(" + width / 1280 + "," + height / 766 + ")");

  $(".menu").hide();
  $(".actions .options .hit-area").click(function() {
    $(".menu").fadeToggle(500);
    $(this).toggleClass("active")
  });

  function enumerateAllProps(obj) {
    var msg = '';
    var props = Object.getOwnPropertyNames(obj);
    for (var j = 0; j < props.length; ++j) {
      msg += props[j] + ': ' + obj[props[j]];
    }
    return msg;
  }

  var ardrone = xwalk.experimental.ardrone;

  var isConnected = false;
  $("#btnConnect").click(function() {
    if (isConnected) {
      ardrone.quit().then(function(msg) {
        console.log(enumerateAllProps(msg));
      }, null);
      $(".button-wrap .button.power .hit-area").css("background-color", "#FF3030");
      isConnected = false;
    } else {
      ardrone.connect().then(function(msg) {
        console.log(enumerateAllProps(msg));
      }, null);
      $(".button-wrap .button.power .hit-area").css("background-color", "#66CD00");
      isConnected = true;
    }
  });

  $("#btnFtrim").click(function() {
    ardrone.ftrim().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

  $("#btnTakeoff").click(function() {
    ardrone.takeoff().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

  $("#btnLanding").click(function() {
    ardrone.landing().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

  $("#btnControl").draggable({
    revert: true,
    containment: "parent",
    create: function() {
      $(this).data("startLeft",parseInt($(this).css("left")));
      $(this).data("startTop",parseInt($(this).css("top")));
    },
    drag: function(event, ui) {
      var rel_left = ui.position.left - parseInt($(this).data("startLeft"));
      var rel_top = ui.position.top - parseInt($(this).data("startTop"));

      // handle left/right movement
      if (ui.position.left < 15) {
        // stop going right
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
        // go left
        ardrone.roll_plus().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      } else if (ui.position.left > 45) {
        // stop going left
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
        // go right
        ardrone.roll_minus().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      } else {
        // stop going left or right
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      }

      // handle forward/backward movement
      if (ui.position.top < 15) {
        // stop going backward
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
        // go forward
        ardrone.pitch_plus().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      } else if (ui.position.top > 45) {
        // stop going forward
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
        // go backward
        ardrone.pitch__minus().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      } else {
        // stop going forward or backward
        ardrone.hover().then(function(msg) {
          console.log(enumerateAllProps(msg));
        }, null);
      }
    },
    stop: function(){
      ardrone.hover().then(function(msg) {
        console.log(enumerateAllProps(msg));
      }, null);
    }
  });

  $("#btnHover").click(function() {
    ardrone.hover().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

  $("#btnClockwise").click(function() {
    ardrone.yaw_plus().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

  $("#btnCounterclockwise").click(function() {
    ardrone.yaw_minus().then(function(msg) {
      console.log(enumerateAllProps(msg));
    }, null);
  });

});
