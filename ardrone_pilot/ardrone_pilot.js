// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var g_next_async_call_id = 0;
var g_async_calls = [];
var g_listeners = [];

var g_next_listener_id = 0;

function AsyncCall(resolve, reject) {
  this.resolve = resolve;
  this.reject = reject;
}

function createPromise(msg) {
  var promise = new Promise(function(resolve, reject) {
    g_async_calls[g_next_async_call_id] = new AsyncCall(resolve, reject);
  });
  msg.asyncCallId = g_next_async_call_id;
  extension.postMessage(JSON.stringify(msg));
  ++g_next_async_call_id;
  return promise;
}

exports.connect = function() {
  var msg = {
    'cmd': 'connect'
  };
  return createPromise(msg);
};

exports.quit = function() {
  var msg = {
    'cmd': 'quit'
  };
  return createPromise(msg);
};

exports.ftrim = function() {
  var msg = {
    'cmd': 'ftrim'
  };
  return createPromise(msg);
};

exports.takeoff = function() {
  var msg = {
    'cmd': 'takeoff'
  };
  return createPromise(msg);
};

exports.landing = function() {
  var msg = {
    'cmd': 'landing'
  };
  return createPromise(msg);
};

exports.hover = function() {
  var msg = {
    'cmd': 'hover'
  };
  return createPromise(msg);
};

exports.pitch_plus = function() {
  var msg = {
    'cmd': 'pitch_plus'
  };
  return createPromise(msg);
};

exports.pitch_minus = function() {
  var msg = {
    'cmd': 'pitch_minus'
  };
  return createPromise(msg);
};

exports.hover = function() {
  var msg = {
    'cmd': 'hover'
  };
  return createPromise(msg);
};

exports.roll_plus = function() {
  var msg = {
    'cmd': 'roll_plus'
  };
  return createPromise(msg);
};

exports.roll_minus = function() {
  var msg = {
    'cmd': 'roll_minus'
  };
  return createPromise(msg);
};

exports.yaw_plus = function() {
  var msg = {
    'cmd': 'yaw_plus'
  };
  return createPromise(msg);
};

exports.yaw_minus = function() {
  var msg = {
    'cmd': 'yaw_minus'
  };
  return createPromise(msg);
};

extension.setMessageListener(function(json) {
  var msg = JSON.parse(json);

  if (msg.data.error) {
    g_async_calls[msg.asyncCallId].reject(msg.data.error);
  } else {
    g_async_calls[msg.asyncCallId].resolve(msg.data); 
  }

  delete g_async_calls[msg.asyncCallId];
});

var _sendSyncMessage = function(msg) {
  return extension.internal.sendSyncMessage(JSON.stringify(msg));
};
