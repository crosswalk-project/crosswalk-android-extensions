// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var g_asyncRequests = {};
var g_nextRequestId = 0;
var g_initialized = false;

exports.__defineGetter__("initialized", function() {
  return g_initialized;
});

function AsyncRequest(resolve, reject) {
  this.resolve = resolve;
  this.reject = reject;
}

function DOMError(name, message) {
  this.name = name;
  this.message = message;
}

function createAsyncRequest(resolve, reject) {
  var requestId = ++g_nextRequestId;
  var asyncRequest = new AsyncRequest(resolve, reject);
  g_asyncRequests[requestId] = asyncRequest;

  return requestId;
}

function sendAsycRequest(command, requestId, message) {
  var message = { "cmd": command, "requestId": requestId, "data": message };
  extension.postMessage(JSON.stringify(message));
}

exports.init = function(options) {
  return new Promise(function(resolve, reject) {
    if (g_initialized)
      throw new DOMError("InvalidStateError");
    if (typeof(options.channel) === "undefined")
      throw new DOMError("InvalidAccessError");
    var resolveWrapper = function() {
      g_initialized = true;
      resolve();
    }
    var requestId = createAsyncRequest(resolveWrapper, reject);
    sendAsycRequest("init", requestId, options);
  });
}

exports.queryProductsInfo = function(productIds) {
  return new Promise(function(resolve, reject) {
    if (!g_initialized) {
      throw new DOMError("InvalidStateError");
    }
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("queryProductsInfo", requestId, productIds);
  });
}


exports.purchase = function(order) {
  return new Promise(function(resolve, reject) {
    if (!g_initialized) {
      throw new DOMError("InvalidStateError");
    }
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("purchase", requestId, order);
  });
}

exports.getReceipt = function() {
  return new Promise(function(resolve, reject) {
    if (!g_initialized) {
      throw new DOMError("InvalidStateError");
    }
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("getReceipt", requestId);
  });
}

exports.validateReceipt = function() {
  return new Promise(function(resolve, reject) {
    if (!g_initialized) {
      throw new DOMError("InvalidStateError");
    }
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("validateReceipt", requestId);
  });
}

exports.restore = function() {
  return new Promise(function(resolve, reject) {
    if (!g_initialized) {
      throw new DOMError("InvalidStateError");
    }
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("restore", requestId);
  });
}

extension.setMessageListener(function(json) {
  var msg = JSON.parse(json);
  var request = g_asyncRequests[msg.requestId];

  if (request) {
    if (msg.error) {
      request.reject.apply(null, [msg.error]);
    } else {
      request.resolve.apply(null, [msg.data]);
    }
    delete g_asyncRequests[msg.requestId];
  }
});
