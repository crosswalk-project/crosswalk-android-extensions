// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var g_async_calls = [];
var g_next_async_call_id = 0;

var g_listeners = [];
var g_next_listener_id = 0;

var g_video = null;
var g_canvas_timer = null;
var g_last_mp4_file = null;

var ARDroneVideoCodec = {
  // A MJPG-like codec, which was the default one until 1.6.4.
  VLIB: 1,
  // A h264-like codec, which should be the default one after 1.6.4.
  P264: 2
};

window.ARDroneVideoCodec = ARDroneVideoCodec;

var ARDroneVideoChannel = {
  ZAP_CHANNEL_HORI : 1,
  ZAP_CHANNEL_VERT : 2,
  ZAP_CHANNEL_LARGE_HORI_SMALL_VERT : 3,
  ZAP_CHANNEL_LARGE_VERT_SMALL_HORI : 4
};

window.ARDroneVideoChannel = ARDroneVideoChannel;

function ARDroneVideoOption () {
  this.ipAddress = '192.168.1.1';
  this.port = 5555;
  this.latency = -1; // milliseconds to update mp4 files, -1 means no control
  this.bitrate = 15000; // also called fps, in millisecond
  this.codec = ARDroneVideoCodec.P264;
  this.channel = ARDroneVideoChannel.ZAP_CHANNEL_HORI;
};

window.ARDroneVideoOption = ARDroneVideoOption;

exports.isPlaying = false;

exports.play = function(idOfCanvas, option) {
  if (exports.isPlaying) {
    console.log('Video is playing, please stop first.');
    return;
  }

  var canvas = document.getElementById(idOfCanvas);
  if (!canvas) {
    console.log('Invalid canvas id: ' + idOfCanvas);
    return;
  }

  var ctx = canvas.getContext('2d');
  if (!ctx) {
    console.log('Failed to get canvas context, is ' + idOfCanvas + ' a valid canvas element?');
    return;
  }

  // Create a tempory hidden video element
  g_video = document.createElement('video');
  g_video.removeAttribute("controls");
  g_video.removeAttribute("autoplay");
  g_video.setAttribute("hidden", "hidden");
  g_video.setAttribute("preload", "auto");

  document.body.appendChild(g_video);

  function _updateCanvas() {
    if (!g_video.paused && !g_video.ended) {
      ctx.drawImage(g_video, 0, 0, canvas.width, canvas.height);
    }
  }

  g_video.addEventListener('play', function() {
    _clearCanvasTimer();

    // TODO(halton): hard-code 30 FPS
    var timer = window.requestInterval(_updateCanvas, 1000 / 30);

    g_canvas_timer = new Object();
    g_canvas_timer.value = timer.value;
  }, true);

  exports.addEventListener('newvideoready', function(e) {
    g_video.pause();
    _removeLastMp4File();
    g_last_mp4_file = e.absolutePath;

    g_video.src = 'file://' + e.absolutePath;
    g_video.play();
  });

  if (_isARDroneVideoOption(option)) {
    exports.option = option;
  } else if (!_isARDroneVideoOption(exports.option)) {
    exports.option = new ARDroneVideoOption();
  }

  var msg = {
    'cmd': 'play',
    'option': exports.option
  };

  return _createPromise(msg);
};

function _clearCanvasTimer() {
  if (g_canvas_timer && g_canvas_timer.value)
    window.clearRequestInterval(g_canvas_timer);

  g_canvas_timer = null;
}

function _removeLastMp4File() {
  exports.removeFile(g_last_mp4_file)
    .then(function() { },
          function(e) { console.log(e.message); }
         );
}

function _cleanup() {
  _clearCanvasTimer();
  if (g_video) {
    document.body.removeChild(g_video);
    g_video = null;
  }

  _removeLastMp4File();
  g_last_mp4_file = null;
}

exports.stop = function() {
  if (!exports.isPlaying) {
    console.log('Video is not playing, nothing to do.');
    return;
  }

  _cleanup();

  var msg = {
    'cmd': 'stop'
  };
  return _createPromise(msg);
};

exports.removeFile = function(path) {
  var msg = {
    'cmd': 'removeFile',
    'path': path
  };
  return _createPromise(msg);
};

function _addConstProperty(obj, propertyKey, propertyValue) {
  Object.defineProperty(obj, propertyKey, {
    configurable: false,
    writable: false,
    value: propertyValue
  });
}

window.ARDroneVideoEvent = function(data) {
  _addConstProperty(this, 'absolutePath', data.absolutePath);
  this.prototype = new Event('ARDroneVideoEvent');
};

extension.setMessageListener(function(json) {
  var msg = JSON.parse(json);

  // Handle events
  if (msg.reply == 'newvideoready') {
    for (var id in g_listeners) {
      var event = new ARDroneVideoEvent(msg.data);
      g_listeners[id]['callback'](event);
    }
    return;
  }

  // Handle promises
  if (msg.data.error) {
    g_async_calls[msg.asyncCallId].reject(msg.data.error);
  } else {
    g_async_calls[msg.asyncCallId].resolve(msg.data); 
    exports.isPlaying = g_async_calls[msg.asyncCallId].type === 'play';
  }

  delete g_async_calls[msg.asyncCallId];
});

exports.addEventListener = function(eventName, callback) {
  if (eventName != 'newvideoready') {
    console.log('Unsupportted event: ' + eventName);
    return;
  }

  var listener = {
    'eventName': eventName,
    'callback': callback
  };

  var listener_id = g_next_listener_id;
  g_next_listener_id += 1;
  g_listeners[listener_id] = listener;
};

function _AsyncCall(type, resolve, reject) {
  this.type = type;
  this.resolve = resolve;
  this.reject = reject;
}

function _createPromise(msg) {
  var promise = new Promise(function(resolve, reject) {
    g_async_calls[g_next_async_call_id] = new _AsyncCall(msg.cmd, resolve, reject);
  });

  msg.asyncCallId = g_next_async_call_id;
  extension.postMessage(JSON.stringify(msg));
  ++g_next_async_call_id;
  return promise;
}

function _defineReadOnlyProperty(object, key, value) {
  Object.defineProperty(object, key, {
    configurable: false,
    writable: false,
    value: value
  });
}

function _isARDroneVideoOption(option) {
  if (!option) return false;

  var tmp_option = new ARDroneVideoOption();
  for (var key in tmp_option)
    if (!option.hasOwnProperty(key))
      return false;

  return true;
}

// requestAnimationFrame() shim by Paul Irish
// http://paulirish.com/2011/requestanimationframe-for-smart-animating/
// Copied from https://gist.github.com/joelambert/1002116
window.requestAnimFrame = (function() {
  return window.requestAnimationFrame       ||
         window.webkitRequestAnimationFrame ||
         window.mozRequestAnimationFrame    ||
         window.oRequestAnimationFrame      ||
         window.msRequestAnimationFrame     ||
         function(/* function */ callback, /* DOMElement */ element){
           window.setTimeout(callback, 1000 / 60)
         };
})();

/**
 * Copied from https://gist.github.com/joelambert/1002116
 *
 * Behaves the same as setInterval except uses requestAnimationFrame() where possible for better performance
 * @param {function} fn The callback function
 * @param {int} delay The delay in milliseconds
 */
window.requestInterval = function(fn, delay) {
  if (!window.requestAnimationFrame &&
      !window.webkitRequestAnimationFrame &&
      // Firefox 5 ships without cancel support
      !(window.mozRequestAnimationFrame && window.mozCancelRequestAnimationFrame) &&
      !window.oRequestAnimationFrame &&
      !window.msRequestAnimationFrame)
      return window.setInterval(fn, delay);

  var start = new Date().getTime(),
      handle = new Object();

  function loop() {
    var current = new Date().getTime(),
        delta = current - start;

    if (delta >= delay) {
      fn.call();
      start = new Date().getTime();
    }

    handle.value = requestAnimFrame(loop);
  };

  handle.value = requestAnimFrame(loop);
  return handle;
}

/**
 * Copied from https://gist.github.com/joelambert/1002116
 *
 * Behaves the same as clearInterval except uses cancelRequestAnimationFrame() where possible for better performance
 * @param {int|object} fn The callback function
 */
window.clearRequestInterval = function(handle) {
  window.cancelAnimationFrame              ? window.cancelAnimationFrame(handle.value) :
  window.webkitCancelAnimationFrame        ? window.webkitCancelAnimationFrame(handle.value) :
  /* Support for legacy API */
  window.webkitCancelRequestAnimationFrame ? window.webkitCancelRequestAnimationFrame(handle.value) :
  window.mozCancelRequestAnimationFrame    ? window.mozCancelRequestAnimationFrame(handle.value) :
  window.oCancelRequestAnimationFrame      ? window.oCancelRequestAnimationFrame(handle.value) :
  window.msCancelRequestAnimationFrame     ? window.msCancelRequestAnimationFrame(handle.value) :
  clearInterval(handle);
};
