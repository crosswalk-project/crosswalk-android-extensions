// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var _promises = {};
var _next_promise_id = 0;

var Promise = requireNative('sysapps_promise').Promise;

var _postMessage = function(msg) {
  var p = new Promise();

  _promises[_next_promise_id] = p;
  msg._promise_id = _next_promise_id.toString();
  _next_promise_id += 1;

  extension.postMessage(JSON.stringify(msg));
  return p;
};

extension.setMessageListener(function(json) {
  var msg = JSON.parse(json);

  if (msg.data && msg.data.error) {
    _promises[msg._promise_id].reject(msg.data.error);
  } else {
    _promises[msg._promise_id].fulfill(msg.data);
  }

  delete _promises[msg._promise_id];
});

// [IN] key: a base64 encoded public key that identifies the application
// This key can be found in the Google Play developer console
exports.init = function(key) {
  var msg = {
    'cmd': 'init',
    'key': key
  };
  return _postMessage(msg);
}

// [IN] productIds: a list of ids to be queried
exports.queryProductDetails = function(productIds) {
  var msg = {
    'cmd': 'query_products',
    'ids': JSON.stringify(productIds)
  };
  return _postMessage(msg);
}

// [IN] productId: id of the item which to be purchased
exports.buy = function(productId) {
  var msg = {
    'cmd': 'buy',
    'id': productId
  };
  return _postMessage(msg);
}
