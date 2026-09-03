const ws = new WebSocket('wss://koushik-trading-ai.onrender.com/ws/market');

ws.onopen = () => {
    console.log('[NODE WS CONNECT] Connected to backend');
    const sub = JSON.stringify({ action: 'subscribe', exchangeType: 1, tokens: ['99926000', '99926009', '2885', '1333', '11536'] });
    ws.send(sub);
    console.log('[NODE WS SUBSCRIBE] Subscribed:', sub);
};

let msgCount = 0;
ws.onmessage = async (event) => {
    msgCount++;
    const now = new Date().toISOString();
    if (typeof event.data === 'string') {
        console.log('[NODE WS MSG TEXT #' + msgCount + ' at ' + now + ']:', event.data);
    } else {
        const buf = Buffer.from(await event.data.arrayBuffer());
        console.log('[NODE WS MSG BINARY #' + msgCount + ' len=' + buf.length + ' at ' + now + ']: hex=' + buf.subarray(0, 30).toString('hex'));
    }
};

ws.onerror = (e) => console.error('WS Error:', e);
ws.onclose = (e) => console.log('WS Closed:', e.code, e.reason);

setTimeout(() => {
    console.log('[DONE] Total messages received in 20s:', msgCount);
    ws.close();
    process.exit(0);
}, 20000);
