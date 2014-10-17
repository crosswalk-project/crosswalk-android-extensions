function handleCalculate(msg) {
  if (typeof msg.sum === 'undefined') {
    console.log('Received unexpected msg: ' + JSON.stringify(msg));
    return;
  }
  g_async_calls[msg.asyncCallId].resolve(msg.sum * 2);
}
