// Copyright (c) 2016 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var g_asyncRequests = {};
var g_nextRequestId = 0;

function AsyncRequest(resolve, reject) {
  this.resolve = resolve;
  this.reject = reject;
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

exports.authenticate = function(reason) {
  return new Promise(function(resolve, reject) {
    var requestId = createAsyncRequest(resolve, reject);
    sendAsycRequest("authenticate", requestId, reason);
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
