// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var g_async_calls = [];
var g_next_async_call_id = 0;

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

function _isFunction(fn) {
  return !!fn && !fn.nodeName && fn.constructor != String
    && fn.constructor != RegExp && fn.constructor != Array
    && /function/i.test( fn + "" );
}

var adList = [];

function Advertise(id, service) {
  _defineReadOnlyProperty(this, "id", id);
  _defineReadOnlyProperty(this, "service", service);
}

Advertise.prototype.destroy = function() {
  var _msg = {
    cmd: "destroy",
    data: {
      "id" : this.id,
      "service" : this.service
    }
  };
  return _createPromise(_msg);
}

Advertise.prototype.show = function(shown) {
  var _msg = {
    cmd: "show",
    data: {
      "id" : this.id,
      "service" : this.service,
      "shown" : shown
    }
  };
  return _createPromise(_msg);
}

extension.setMessageListener(function(json) {
  var msg = JSON.parse(json);
  if (msg.error) {
    if (!!msg.asyncCallId &&
        !!g_async_calls[msg.asyncCallId] &&
        _isFunction(g_async_calls[msg.asyncCallId].reject))
      g_async_calls[msg.asyncCallId].reject(msg.data);
    return;
  }
  switch (msg.cmd) {
    case "create_ret" : {
      var data = new Advertise(msg.data.id, msg.data.service);
      adList[data.id] = data;
      if (!!msg.asyncCallId &&
          !!g_async_calls[msg.asyncCallId] &&
          _isFunction(g_async_calls[msg.asyncCallId].resolve))
        g_async_calls[msg.asyncCallId].resolve(data);
      break;
    }
    case "onclosed" :
    case "onfailed" :
    case "onopened" :
    case "onloaded" : {
      var ad = adList[msg.data.id];
      if (ad && _isFunction(ad[msg.cmd])) {
        ad[msg.cmd](msg.data.body);
      }
      break;
    }
    default:
      if (msg.cmd)
        console.log("Error: wrong command \'" + msg.cmd + "\'");
  }
  if (!!msg.asyncCallId && !!g_async_calls[msg.asyncCallId])
    delete g_async_calls[msg.asyncCallId];
});

exports.create= function (requestOptions) {
  var msg = {
    "cmd" : "create",
    "data" : requestOptions
  }
  return _createPromise(msg);
};
