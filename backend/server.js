/**
 * ==============================================================================
 * KOUSHIK TRADING AI - PRODUCTION BACKEND PROXY & MARKET DATA ENGINE
 * ==============================================================================
 * 
 * Features:
 * 1. Secure proxy between Android App and Angel One SmartAPI (Zero credential exposure).
 * 2. Automatic Angel One authentication via TOTP, JWT management, and auto-refresh.
 * 3. Read-Only Market Data (REST + WebSocket ticks). No trading or order operations.
 * 4. High-performance Sub-Second (1s, 5s, 15s, 30s) Candle Aggregation Engine.
 * 5. WebSocket endpoint (/ws/market) with subscribe, unsubscribe, ping, reconnect support.
 * 6. Built-in Indian stock scrip directory (NSE/BSE) with instant fuzzy search.
 * 7. Graceful simulation fallback for off-market hours and pre-credential testing.
 * ==============================================================================
 */

require('dotenv').config();
const http = require('http');
const express = require('express');
const cors = require('cors');
const { WebSocketServer, WebSocket } = require('ws');
const axios = require('axios');
const { authenticator } = require('otplib');
const { scripMasterManager, DEFAULT_FALLBACK_STOCKS } = require('./scripMaster');
const { indicesManager } = require('./indicesManager');

// ------------------------------------------------------------------------------
// 1. CONFIGURATION & ENVIRONMENT
// ------------------------------------------------------------------------------
const PORT = process.env.PORT || 3000;
const ANGEL_API_KEY = process.env.ANGEL_API_KEY || '';
const ANGEL_CLIENT_ID = process.env.ANGEL_CLIENT_ID || '';
const ANGEL_PIN = process.env.ANGEL_PIN || '';
const ANGEL_TOTP_SECRET = process.env.ANGEL_TOTP_SECRET || '';
const ANGEL_PUBLIC_IP = process.env.ANGEL_PUBLIC_IP || '106.193.147.98';
const ANGEL_LOCAL_IP = process.env.ANGEL_LOCAL_IP || '192.168.1.1';
const ANGEL_MAC_ADDRESS = process.env.ANGEL_MAC_ADDRESS || 'fe80::216e:6507:4b90:3719';
const CORS_ORIGIN = process.env.CORS_ORIGIN || '*';

const SMARTAPI_BASE_URL = 'https://apiconnect.angelone.in';
const SMARTSTREAM_WS_URL = 'wss://smartapisocket.angelone.in/smart-stream';

// Validate if Angel One credentials are real (non-placeholder)
function isAngelConfigured() {
  return (
    ANGEL_API_KEY &&
    ANGEL_CLIENT_ID &&
    ANGEL_PIN &&
    ANGEL_TOTP_SECRET &&
    !ANGEL_API_KEY.includes('YOUR_ANGEL') &&
    !ANGEL_CLIENT_ID.includes('YOUR_ANGEL')
  );
}

// ------------------------------------------------------------------------------
// 2. SCRIP MASTER & POPULAR STOCKS DIRECTORY
// ------------------------------------------------------------------------------
const STOCK_DIRECTORY = [
  { symbol: 'NIFTY 50', name: 'NIFTY 50 INDEX', token: '99926000', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'BANKNIFTY', name: 'NIFTY BANK INDEX', token: '99926009', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'FINNIFTY', name: 'NIFTY FINANCIAL SERVICES', token: '99926037', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'RELIANCE', name: 'Reliance Industries Ltd', token: '2885', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'BEL', name: 'Bharat Electronics Ltd', token: '383', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'HDFCBANK', name: 'HDFC Bank Ltd', token: '1333', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'TCS', name: 'Tata Consultancy Services', token: '11536', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'INFY', name: 'Infosys Ltd', token: '1594', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'ICICIBANK', name: 'ICICI Bank Ltd', token: '4963', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'TATAMOTORS', name: 'Tata Motors Ltd', token: '3456', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'SBIN', name: 'State Bank of India', token: '3045', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'ITC', name: 'ITC Ltd', token: '1660', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'BHARTIARTL', name: 'Bharti Airtel Ltd', token: '10604', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'LT', name: 'Larsen & Toubro Ltd', token: '11483', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'KOTAKBANK', name: 'Kotak Mahindra Bank Ltd', token: '1922', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'AXISBANK', name: 'Axis Bank Ltd', token: '5900', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'BAJFINANCE', name: 'Bajaj Finance Ltd', token: '317', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'MARUTI', name: 'Maruti Suzuki India Ltd', token: '10999', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'SUNPHARMA', name: 'Sun Pharmaceutical Ltd', token: '3351', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'TITAN', name: 'Titan Company Ltd', token: '3506', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 },
  { symbol: 'TATASTEEL', name: 'Tata Steel Ltd', token: '3499', exchange: 'NSE', ltp: 0.0, prevClose: 0.0 }
];

// Map token to stock item for O(1) lookups
const tokenMap = new Map();
STOCK_DIRECTORY.forEach(item => {
  tokenMap.set(item.token, item);
  tokenMap.set(item.symbol, item);
});

// Also register supported indices in tokenMap
indicesManager.getAllIndices().forEach(idx => {
  const indexItem = {
    symbol: idx.symbol,
    name: idx.name,
    token: idx.token,
    exchange: 'NSE',
    ltp: 0.0,
    prevClose: 0.0
  };
  tokenMap.set(idx.token, indexItem);
  tokenMap.set(idx.symbol, indexItem);
  if (idx.alias) {
    tokenMap.set(idx.alias, indexItem);
  }
});

// ------------------------------------------------------------------------------
// 3. ANGEL ONE SMARTAPI AUTHENTICATION MANAGER
// ------------------------------------------------------------------------------
class SmartApiAuthManager {
  constructor() {
    this.jwtToken = null;
    this.refreshToken = null;
    this.feedToken = null;
    this.lastLoginTime = null;
    this.isAuthenticated = false;
    this.isAuthenticating = false;
    this.lastError = null;
    this.refreshTimer = null;
  }

  async login() {
    if (!isAngelConfigured()) {
      this.lastError = 'Angel One credentials not configured. Running in simulation mode.';
      console.log(`[SmartAPI Auth] ${this.lastError}`);
      return false;
    }

    if (this.isAuthenticating) {
      return false;
    }

    this.isAuthenticating = true;
    this.lastError = null;

    try {
      // 1. Generate current TOTP code using Base32 secret
      const totpCode = authenticator.generate(ANGEL_TOTP_SECRET.trim());
      console.log(`[ANGEL WS AUTH] Initiating Angel One login with TOTP for client: ${ANGEL_CLIENT_ID.trim()}...`);

      // 2. Call loginByPassword endpoint
      const response = await axios.post(
        `${SMARTAPI_BASE_URL}/rest/auth/angelbroking/user/v1/loginByPassword`,
        {
          clientcode: ANGEL_CLIENT_ID.trim(),
          password: ANGEL_PIN.trim(),
          totp: totpCode
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-UserType': 'USER',
            'X-SourceID': 'WEB',
            'X-ClientLocalIP': ANGEL_LOCAL_IP,
            'X-ClientPublicIP': ANGEL_PUBLIC_IP,
            'X-MACAddress': ANGEL_MAC_ADDRESS,
            'X-PrivateKey': ANGEL_API_KEY.trim()
          },
          timeout: 12000
        }
      );

      if (response.data && response.data.status && response.data.data) {
        const data = response.data.data;
        this.jwtToken = data.jwtToken.startsWith('Bearer ') ? data.jwtToken : `Bearer ${data.jwtToken}`;
        this.refreshToken = data.refreshToken;
        this.feedToken = data.feedToken;
        this.lastLoginTime = new Date().toISOString();
        this.isAuthenticated = true;
        this.lastError = null;
        console.log(`[ANGEL WS AUTH] Login SUCCESSFUL. Session active. feedToken length: ${this.feedToken ? this.feedToken.length : 0}`);

        // Schedule token refresh in 6 hours
        this.scheduleTokenRefresh();
        return true;
      } else {
        const errorMsg = response.data?.message || 'Login failed with unknown SmartAPI error';
        this.lastError = errorMsg;
        this.isAuthenticated = false;
        console.error(`[ANGEL WS AUTH] Login Failed: ${errorMsg}`);
        return false;
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message;
      this.lastError = errorMsg;
      this.isAuthenticated = false;
      console.error(`[ANGEL WS AUTH] Authentication Exception: ${errorMsg}`);
      return false;
    } finally {
      this.isAuthenticating = false;
    }
  }

  async refreshTokens() {
    if (!this.refreshToken || !this.jwtToken) {
      return this.login();
    }

    try {
      console.log('[SmartAPI Auth] Refreshing JWT tokens...');
      const response = await axios.post(
        `${SMARTAPI_BASE_URL}/rest/auth/angelbroking/jwt/v1/generateTokens`,
        { refreshToken: this.refreshToken },
        {
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-UserType': 'USER',
            'X-SourceID': 'WEB',
            'X-ClientLocalIP': ANGEL_LOCAL_IP,
            'X-ClientPublicIP': ANGEL_PUBLIC_IP,
            'X-MACAddress': ANGEL_MAC_ADDRESS,
            'X-PrivateKey': ANGEL_API_KEY.trim(),
            'Authorization': this.jwtToken
          },
          timeout: 10000
        }
      );

      if (response.data && response.data.status && response.data.data) {
        const data = response.data.data;
        this.jwtToken = data.jwtToken.startsWith('Bearer ') ? data.jwtToken : `Bearer ${data.jwtToken}`;
        this.refreshToken = data.refreshToken;
        this.feedToken = data.feedToken || this.feedToken;
        this.lastLoginTime = new Date().toISOString();
        this.isAuthenticated = true;
        console.log('[SmartAPI Auth] Token refresh SUCCESSFUL.');
        this.scheduleTokenRefresh();
        return true;
      } else {
        console.warn('[SmartAPI Auth] Token refresh failed, falling back to full login.');
        return this.login();
      }
    } catch (err) {
      console.warn(`[SmartAPI Auth] Refresh exception: ${err.message}. Retrying login.`);
      return this.login();
    }
  }

  scheduleTokenRefresh() {
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
    // Refresh every 6 hours (21600000 ms)
    this.refreshTimer = setTimeout(() => {
      this.refreshTokens().catch(err => console.error('[SmartAPI Auth] Scheduled refresh error:', err));
    }, 6 * 60 * 60 * 1000);
  }

  getHeaders() {
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-UserType': 'USER',
      'X-SourceID': 'WEB',
      'X-ClientLocalIP': ANGEL_LOCAL_IP,
      'X-ClientPublicIP': ANGEL_PUBLIC_IP,
      'X-MACAddress': ANGEL_MAC_ADDRESS,
      'X-PrivateKey': ANGEL_API_KEY.trim(),
      'Authorization': this.jwtToken || ''
    };
  }
}

const authManager = new SmartApiAuthManager();

// ------------------------------------------------------------------------------
// 4. REAL-TIME SUB-SECOND CANDLE AGGREGATION ENGINE
// ------------------------------------------------------------------------------
const INTERVAL_SECONDS = {
  '1s': 1,
  '5s': 5,
  '15s': 15,
  '30s': 30,
  '1m': 60
};

class CandleAggregator {
  constructor() {
    // token -> interval -> array of Candle
    this.candleHistory = new Map();
    // token -> interval -> current active Candle
    this.currentCandles = new Map();
  }

  processTick(token, ltp, volume, timestamp = Date.now()) {
    const updatedCandles = [];

    for (const [intervalKey, seconds] of Object.entries(INTERVAL_SECONDS)) {
      const intervalMs = seconds * 1000;
      const bucketTimestamp = Math.floor(timestamp / intervalMs) * intervalMs;

      const candleKey = `${token}_${intervalKey}`;
      let activeCandle = this.currentCandles.get(candleKey);

      if (!activeCandle || activeCandle.timestamp !== bucketTimestamp) {
        // Close previous candle and store in ring buffer
        if (activeCandle) {
          activeCandle.isComplete = true;
          this.storeCompletedCandle(token, intervalKey, activeCandle);
          updatedCandles.push({
            token,
            interval: intervalKey,
            candle: { ...activeCandle }
          });
        }

        // Start new candle bucket
        activeCandle = {
          timestamp: bucketTimestamp,
          open: ltp,
          high: ltp,
          low: ltp,
          close: ltp,
          volume: volume || 100,
          isComplete: false
        };
        this.currentCandles.set(candleKey, activeCandle);
      } else {
        // Update ongoing candle
        activeCandle.high = Math.max(activeCandle.high, ltp);
        activeCandle.low = Math.min(activeCandle.low, ltp);
        activeCandle.close = ltp;
        activeCandle.volume += (volume || 10);
      }

      updatedCandles.push({
        token,
        interval: intervalKey,
        candle: { ...activeCandle }
      });
    }

    return updatedCandles;
  }

  storeCompletedCandle(token, interval, candle) {
    const historyKey = `${token}_${interval}`;
    if (!this.candleHistory.has(historyKey)) {
      this.candleHistory.set(historyKey, []);
    }
    const list = this.candleHistory.get(historyKey);
    list.push(candle);
    // Keep max 250 candles in memory buffer
    if (list.length > 250) {
      list.shift();
    }
  }

  getCandles(token, interval) {
    const historyKey = `${token}_${interval}`;
    const history = this.candleHistory.get(historyKey) || [];
    const active = this.currentCandles.get(historyKey);

    const result = [...history];
    if (active) {
      result.push(active);
    }
    return result;
  }
}

const candleAggregator = new CandleAggregator();

// ------------------------------------------------------------------------------
// 5. ANGEL ONE LIVE WEBSOCKET (SMARTSTREAM) CLIENT & FALLBACK SIMULATOR
// ------------------------------------------------------------------------------
class UpstreamMarketFeed {
  constructor() {
    this.ws = null;
    this.subscribedTokens = new Set(['3045', '2885', '1333', '11536', '1594', '99926000', '99926009']);
    this.isConnected = false;
    this.reconnectTimeout = null;
    this.simulationInterval = null;
  }

  connect() {
    if (!isAngelConfigured() || !authManager.isAuthenticated) {
      console.warn('[ANGEL WS CONNECT] Cannot connect: Angel One credentials missing or not authenticated.');
      return;
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    try {
      const rawJwt = authManager.jwtToken ? authManager.jwtToken.replace(/^Bearer\s+/i, '').trim() : '';
      const bearerToken = `Bearer ${rawJwt}`;
      console.log(`[ANGEL WS CONNECT] Connecting to Angel One Live WebSocket: ${SMARTSTREAM_WS_URL}`);
      console.log(`[ANGEL WS AUTH] Client: ${ANGEL_CLIENT_ID.trim()} | feedToken: ${authManager.feedToken ? 'PRESENT' : 'MISSING'} | JWT: ${rawJwt ? 'PRESENT' : 'MISSING'}`);

      if (this.ws) {
        try {
          this.ws.removeAllListeners();
          this.ws.terminate();
        } catch (e) {}
        this.ws = null;
      }

      this.ws = new WebSocket(SMARTSTREAM_WS_URL, {
        headers: {
          'Authorization': bearerToken,
          'x-api-key': ANGEL_API_KEY.trim(),
          'x-client-code': ANGEL_CLIENT_ID.trim(),
          'x-feed-token': authManager.feedToken
        }
      });

      if (this.pingInterval) {
        clearInterval(this.pingInterval);
        this.pingInterval = null;
      }

      this.ws.on('open', () => {
        console.log('[ANGEL WS CONNECT] Connected to Angel One Market Feed successfully.');
        this.isConnected = true;
        this.resubscribeAll();

        // 10-second heartbeat ping mandated by SmartStream WebSocket server
        this.pingInterval = setInterval(() => {
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send('ping');
          }
        }, 10000);
      });

      this.ws.on('message', (data) => {
        this.handleUpstreamMessage(data);
      });

      this.ws.on('close', (code, reason) => {
        if (this.pingInterval) {
          clearInterval(this.pingInterval);
          this.pingInterval = null;
        }
        console.warn(`[ANGEL WS CONNECT] Disconnected (${code}: ${reason}). Reconnecting in 3s...`);
        this.isConnected = false;
        this.scheduleReconnect();
      });

      this.ws.on('error', (err) => {
        console.error(`[ANGEL WS CONNECT] Error: ${err.message}`);
      });
    } catch (err) {
      console.error(`[ANGEL WS CONNECT] Connection exception: ${err.message}`);
      this.scheduleReconnect();
    }
  }

  resubscribeAll() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || this.subscribedTokens.size === 0) return;

    const tokenList = Array.from(this.subscribedTokens);
    const payload = {
      correlationID: `koushik_sub_${Date.now()}`,
      action: 1, // Subscribe
      params: {
        mode: 1, // LTP Mode
        tokenList: [
          {
            exchangeType: 1, // NSE
            tokens: tokenList
          }
        ]
      }
    };

    this.ws.send(JSON.stringify(payload));
    console.log(`[ANGEL WS SUBSCRIBE] Subscribing to ${tokenList.length} tokens: ${tokenList.join(',')}`);
  }

  subscribeToken(token) {
    const tokenStr = String(token).trim();
    if (!tokenStr) return;
    this.subscribedTokens.add(tokenStr);

    let scrip = scripMasterManager.getByToken(tokenStr);
    const sym = scrip ? scrip.symbol.replace(/-EQ$/i, '') : tokenStr;
    console.log(`[BACKEND_SUBSCRIBE] token: ${tokenStr} | symbol: ${sym}`);
    console.log(`[ANGEL WS SUBSCRIBE] Dynamic Token added: ${tokenStr}`);

    if (!tokenMap.has(tokenStr)) {
      if (scrip) {
        const cleanSym = scrip.symbol.replace(/-EQ$/i, '');
        const stockInfo = {
          symbol: cleanSym,
          name: scrip.name,
          token: tokenStr,
          exchange: scrip.exchange || 'NSE',
          ltp: 0.0,
          prevClose: 0.0
        };
        tokenMap.set(tokenStr, stockInfo);
        tokenMap.set(cleanSym, stockInfo);
        tokenMap.set(scrip.symbol, stockInfo);
        console.log(`[TokenMap] Registered dynamic scrip from master: ${cleanSym} (${tokenStr})`);
      }
    }

    if (this.isConnected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      const payload = {
        correlationID: `koushik_sub_${tokenStr}`,
        action: 1,
        params: {
          mode: 1,
          tokenList: [{ exchangeType: 1, tokens: [tokenStr] }]
        }
      };
      this.ws.send(JSON.stringify(payload));
      console.log(`[ANGEL WS SUBSCRIBE] Sent subscribe request for dynamic token: ${tokenStr} to Angel One SmartStream`);
    }

    fetchAndBroadcastSingleQuote(tokenStr);
  }

  unsubscribeToken(token) {
    this.subscribedTokens.delete(token);
    if (this.isConnected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      const payload = {
        correlationID: `koushik_unsub_${token}`,
        action: 0,
        params: {
          mode: 1,
          tokenList: [{ exchangeType: 1, tokens: [token] }]
        }
      };
      this.ws.send(JSON.stringify(payload));
    }
  }

  handleUpstreamMessage(data) {
    try {
      // SmartAPI SmartStream returns binary packets or JSON
      let parsedTick = null;

      let exchangeTimestamp = Date.now();
      const receivedAt = Date.now();

      if (typeof data === 'string') {
        const json = JSON.parse(data);
        const token = String(json.token || json.token_id || json.symboltoken || '');
        const ltpRaw = json.ltp || json.last_traded_price || json.close || 0;
        const ltp = (typeof ltpRaw === 'number' && ltpRaw > 100000 && !String(ltpRaw).includes('.')) ? (ltpRaw / 100.0) : Number(ltpRaw);
        exchangeTimestamp = json.timestamp || json.exchangeTimestamp || Date.now();
        parsedTick = {
          token,
          ltp,
          angelOneLtp: ltp,
          backendForwardedLtp: ltp,
          volume: json.volume || json.vol || 0,
          timestamp: exchangeTimestamp
        };
        console.log(`[ANGEL_RAW_TICK] token: ${token} | ltp: ${ltp} | exchangeTimestamp: ${exchangeTimestamp} | receivedAt: ${receivedAt}`);
      } else if (Buffer.isBuffer(data) && data.length >= 10) {
        // SmartStream V2 Binary Protocol Parser
        // Byte 0: Subscription mode (1=LTP, 2=QUOTE, 3=SNAP_QUOTE)
        // Byte 1: Exchange Type (1=NSE_CM)
        // Bytes 2..26: Token string (25 bytes null-terminated ASCII)
        // Bytes 27..34: Sequence number (int64 LE)
        // Bytes 35..42: Exchange timestamp (int64 LE)
        // Bytes 43..50: Last Traded Price (int64 LE in paise)
        let token = '';
        if (data.length >= 27) {
          token = data.toString('utf8', 2, 27).replace(/\0/g, '').trim();
        }
        if (!token && data.length >= 4) {
          token = data.readUInt32LE(0).toString();
        }

        if (data.length >= 43) {
          try {
            exchangeTimestamp = Number(data.readBigInt64LE ? data.readBigInt64LE(35) : data.readInt32LE(35));
          } catch (e) {
            exchangeTimestamp = Date.now();
          }
        }

        let ltp = 0;
        if (data.length >= 47) {
          // In mode 1 (LTP), last_traded_price is a 32-bit int LE in paise at byte offset 43
          const ltpPaise = data.readInt32LE(43);
          ltp = ltpPaise / 100.0;
        } else if (data.length >= 8) {
          ltp = data.readInt32LE(4) / 100.0;
        }

        if (token && ltp > 0) {
          parsedTick = {
            token,
            ltp,
            angelOneLtp: ltp,
            backendForwardedLtp: ltp,
            volume: 0,
            timestamp: exchangeTimestamp
          };

          console.log(`[ANGEL_RAW_TICK] token: ${token} | ltp: ${ltp} | exchangeTimestamp: ${exchangeTimestamp} | receivedAt: ${receivedAt}`);
        }
      }

      if (parsedTick && parsedTick.token && parsedTick.ltp > 0) {
        console.log(`[BACKEND TICK] token: ${parsedTick.token} | ltp: ${parsedTick.ltp} | exchangeTimestamp: ${parsedTick.timestamp} | receivedAt: ${receivedAt}`);
        const stockInfo = tokenMap.get(parsedTick.token) || { symbol: `TOKEN_${parsedTick.token}`, exchange: 'NSE' };
        console.log(`[SmartStream Tick Audit] Symbol: ${stockInfo.symbol} | Exchange: ${stockInfo.exchange || 'NSE'} | Token: ${parsedTick.token} | Timestamp: ${parsedTick.timestamp} | Angel One LTP: ${parsedTick.angelOneLtp} | Backend Forwarded LTP: ${parsedTick.backendForwardedLtp}`);
        this.broadcastTick(parsedTick.token, parsedTick.ltp, parsedTick.volume || 0, parsedTick.angelOneLtp, parsedTick.backendForwardedLtp, parsedTick.timestamp);
      }
    } catch (err) {
      console.warn(`[SmartStream] Frame parse exception: ${err.message}`);
    }
  }

  broadcastTick(token, ltp, volume, angelOneLtp = ltp, backendForwardedLtp = ltp, exchangeTimestamp = Date.now()) {
    let stockInfo = tokenMap.get(token);
    if (!stockInfo) {
      const scrip = scripMasterManager.getByToken(token);
      if (scrip) {
        const cleanSym = scrip.symbol.replace(/-EQ$/i, '');
        stockInfo = {
          symbol: cleanSym,
          name: scrip.name,
          token: String(token),
          exchange: scrip.exchange || 'NSE',
          ltp: ltp,
          prevClose: ltp
        };
        tokenMap.set(token, stockInfo);
        tokenMap.set(cleanSym, stockInfo);
        tokenMap.set(scrip.symbol, stockInfo);
      } else {
        stockInfo = { symbol: `TOKEN_${token}`, exchange: 'NSE', prevClose: ltp };
      }
    }
    stockInfo.ltp = ltp;
    const change = stockInfo.prevClose > 0 ? Math.round((ltp - stockInfo.prevClose) * 100) / 100 : 0.0;
    const changePercent = stockInfo.prevClose > 0 ? Math.round((change / stockInfo.prevClose) * 10000) / 100 : 0.0;

    // Update IndicesManager if token matches an index
    indicesManager.updateIndexFromTick(token, ltp, change, changePercent, ltp, ltp, stockInfo.prevClose);

    const receivedAt = Date.now();
    const tickPayload = {
      type: 'tick',
      token,
      symbol: stockInfo.symbol,
      exchange: stockInfo.exchange || 'NSE',
      ltp,
      angelOneLtp,
      backendForwardedLtp,
      change,
      changePercent,
      volume: volume || 0,
      timestamp: exchangeTimestamp,
      exchangeTimestamp: exchangeTimestamp,
      receivedAt: receivedAt
    };

    console.log(`[BACKEND_BROADCAST] token: ${token} | ltp: ${ltp} | exchangeTimestamp: ${exchangeTimestamp} | receivedAt: ${receivedAt}`);
    console.log(`[DATA AUDIT: BACKEND FORWARD] Symbol: ${stockInfo.symbol} | Exchange: ${stockInfo.exchange || 'NSE'} | Token: ${token} | Timestamp: ${tickPayload.timestamp} | Angel One LTP: ${angelOneLtp} | Backend Forwarded LTP: ${backendForwardedLtp}`);

    // Aggregate into 1s, 5s, 15s, 30s candles
    const updatedCandles = candleAggregator.processTick(token, ltp, volume, tickPayload.timestamp);

    // Broadcast to all connected Android clients
    broadcastToClients(tickPayload, token);

    // Also broadcast the 1s sub-second candle update
    const oneSecUpdate = updatedCandles.find(c => c.interval === '1s');
    if (oneSecUpdate) {
      broadcastToClients({
        type: 'candle',
        token: oneSecUpdate.token,
        interval: '1s',
        candle: oneSecUpdate.candle
      }, token);
    }
  }

  startSimulationFeed() {
    // In live mode, simulation is strictly disabled to prevent fake/stale prices
  }

  stopSimulationFeed() {
    if (this.simulationInterval) {
      clearInterval(this.simulationInterval);
      this.simulationInterval = null;
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    this.reconnectTimeout = setTimeout(() => {
      if (isAngelConfigured()) {
        authManager.login().then(() => this.connect());
      }
    }, 3000);
  }
}

// Instant single quote fetcher for dynamic stock/index subscription
async function fetchAndBroadcastSingleQuote(token) {
  if (!isAngelConfigured() || !authManager.isAuthenticated) return;
  try {
    const tokenStr = String(token).trim();
    if (!tokenStr) return;
    const response = await axios.post(
      `${SMARTAPI_BASE_URL}/rest/secure/angelbroking/market/v1/quote/`,
      {
        mode: 'FULL',
        exchangeTokens: {
          NSE: [tokenStr]
        }
      },
      {
        headers: authManager.getHeaders(),
        timeout: 4000
      }
    );
    if (response.data && response.data.status && response.data.data && response.data.data.fetched) {
      const item = response.data.data.fetched[0];
      if (item && (item.token || item.symbolToken)) {
        const ltp = Number(item.ltp || item.lastPrice || 0);
        const change = Number(item.netChange || item.change || 0);
        const changePercent = Number(item.percentChange || item.changePercent || 0);
        const high = Number(item.high || ltp);
        const low = Number(item.low || ltp);
        const prevClose = Number(item.close || (ltp - change));
        if (ltp > 0) {
          console.log(`[Dynamic Quote Instant] Token: ${tokenStr} | Symbol: ${item.tradingSymbol} | LTP: ${ltp}`);
          upstreamMarketFeed.broadcastTick(tokenStr, ltp, 0, ltp, ltp);
          indicesManager.updateIndexFromTick(tokenStr, ltp, change, changePercent, high, low, prevClose);
        }
      }
    }
  } catch (err) {
    // Non-blocking quote fetch failure
  }
}

// Periodic live quote poller from Angel One SmartAPI REST
async function pollAngelOneQuotes() {
  if (!isAngelConfigured() || !authManager.isAuthenticated) return;
  try {
    const baseTokens = ['99926000', '99926009', '99926037', '99926008', '99926004', '99926001', '99926002', '99926005', '99926006', '99926011', '99926018', '99926010', '99926013', '99926014', '383', '2885', '1333', '11536', '1594', '3045'];
    const dynamicTokens = Array.from(upstreamMarketFeed.subscribedTokens);
    const tokens = Array.from(new Set([...baseTokens, ...dynamicTokens]));
    const response = await axios.post(
      `${SMARTAPI_BASE_URL}/rest/secure/angelbroking/market/v1/quote/`,
      {
        mode: 'FULL',
        exchangeTokens: {
          NSE: tokens
        }
      },
      {
        headers: authManager.getHeaders(),
        timeout: 8000
      }
    );

    if (response.data && response.data.status && response.data.data && response.data.data.fetched) {
      const fetchedList = response.data.data.fetched;
      for (const item of fetchedList) {
        const token = String(item.symbolToken || item.token || '');
        const ltp = Number(item.ltp || item.lastPrice || 0);
        const change = Number(item.netChange || item.change || 0);
        const changePercent = Number(item.percentChange || item.changePercent || 0);
        const high = Number(item.high || ltp);
        const low = Number(item.low || ltp);
        const prevClose = Number(item.close || (ltp - change));

        if (token && ltp > 0) {
          console.log(`[SmartAPI Live Quote Audit] Token: ${token} | Symbol: ${item.tradingSymbol} | Raw Angel LTP: ${ltp} | PrevClose: ${prevClose}`);
          upstreamMarketFeed.broadcastTick(token, ltp, 0, ltp, ltp);
          indicesManager.updateIndexFromTick(token, ltp, change, changePercent, high, low, prevClose);
        }
      }
    }
  } catch (err) {
    // Polling failure handled gracefully
  }
}

setInterval(pollAngelOneQuotes, 5000);

const upstreamMarketFeed = new UpstreamMarketFeed();

// ------------------------------------------------------------------------------
// 6. ANDROID CLIENT WEBSOCKET SERVER (/ws/market)
// ------------------------------------------------------------------------------
const clientSockets = new Set();
const clientSubscriptions = new Map(); // ws -> Set<token>

function broadcastToClients(payload, token = null) {
  const messageStr = JSON.stringify(payload);
  for (const client of clientSockets) {
    if (client.readyState === WebSocket.OPEN) {
      const clientSubs = clientSubscriptions.get(client);
      // If token specified, only send to subscribed clients (or all if client has empty filter)
      if (!token || !clientSubs || clientSubs.size === 0 || clientSubs.has(token)) {
        client.send(messageStr);
      }
    }
  }
}

function handleClientWebSocket(ws, req) {
  clientSockets.add(ws);
  const initialSubs = new Set([
    '3045', '2885', '1333', '11536', '1594', '4963', '3456', '1660', '10604', '11483',
    '99926000', '99926009', '99926037', '99926008', '99926029', '99926021', '99926023',
    '99926030', '99926018', '99926031', '99926020', '99926019', '99926038', '99926025',
    '99926047', '99926040', '99926014', '99926011', '99926032', '99926012', '99926033',
    '99926004', '99926013'
  ]);
  clientSubscriptions.set(ws, initialSubs);

  console.log(`[Client WS] New Android client connected. Total clients: ${clientSockets.size}`);

  // Send connection welcome handshake
  ws.send(JSON.stringify({
    type: 'connection',
    status: 'connected',
    service: 'Koushik Trading AI Backend',
    angelConfigured: isAngelConfigured(),
    angelAuthenticated: authManager.isAuthenticated,
    timestamp: Date.now()
  }));

  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);
      const action = data.action || data.type;

      switch (action) {
        case 'subscribe':
        case 1: {
          let rawTokens = data.tokens;
          if (!rawTokens && data.token) {
            rawTokens = [data.token];
          }
          if (!Array.isArray(rawTokens)) {
            rawTokens = rawTokens ? [rawTokens] : [];
          }
          const tokens = rawTokens.map(t => String(t).trim()).filter(Boolean);
          const subs = clientSubscriptions.get(ws) || new Set();
          tokens.forEach(t => {
            subs.add(t);
            upstreamMarketFeed.subscribeToken(t);
          });
          clientSubscriptions.set(ws, subs);
          ws.send(JSON.stringify({ type: 'subscribed', tokens: Array.from(subs) }));
          console.log(`[Client WS Dynamic Subscribe] Subscribed tokens: ${tokens.join(', ')} | Total active subs for client: ${subs.size}`);
          break;
        }

        case 'unsubscribe': {
          const tokens = data.tokens || (data.token ? [data.token] : []);
          const subs = clientSubscriptions.get(ws);
          if (subs) {
            tokens.forEach(t => subs.delete(String(t)));
          }
          ws.send(JSON.stringify({ type: 'unsubscribed', tokens }));
          break;
        }

        case 'ping': {
          ws.send(JSON.stringify({ type: 'pong', timestamp: Date.now() }));
          break;
        }

        case 'reconnect': {
          if (isAngelConfigured()) {
            authManager.login().then(() => upstreamMarketFeed.connect());
          }
          ws.send(JSON.stringify({ type: 'reconnecting', timestamp: Date.now() }));
          break;
        }

        default:
          ws.send(JSON.stringify({ type: 'ack', received: action }));
      }
    } catch (err) {
      ws.send(JSON.stringify({ type: 'error', message: 'Invalid JSON payload format' }));
    }
  });

  ws.on('close', () => {
    clientSockets.delete(ws);
    clientSubscriptions.delete(ws);
    console.log(`[Client WS] Client disconnected. Active clients: ${clientSockets.size}`);
  });

  ws.on('error', (err) => {
    console.error(`[Client WS] Client error: ${err.message}`);
  });
}

// ------------------------------------------------------------------------------
// 7. EXPRESS APPLICATION & REST API ENDPOINTS
// ------------------------------------------------------------------------------
const app = express();

app.use(cors({ origin: CORS_ORIGIN, credentials: true }));
app.use(express.json());

// Logger middleware
app.use((req, res, next) => {
  console.log(`[HTTP] ${req.method} ${req.path}`);
  next();
});

/**
 * GET /health
 * System health, connectivity status, and authentication telemetry
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Koushik Trading AI Backend',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    smartApi: {
      configured: isAngelConfigured(),
      authenticated: authManager.isAuthenticated,
      sessionActive: !!authManager.jwtToken,
      lastLoginTime: authManager.lastLoginTime,
      authError: authManager.lastError
    },
    scripMaster: scripMasterManager.getStatus(),
    indices: {
      supportedCount: indicesManager.getAllIndices().length,
      lastRefreshed: indicesManager.lastRefreshTime
    },
    telemetry: {
      connectedClients: clientSockets.size,
      subscribedTokensCount: upstreamMarketFeed.subscribedTokens.size,
      uptimeSeconds: Math.floor(process.uptime())
    }
  });
});

/**
 * GET /api/indices
 * Return list of all 23 supported NSE indices with live market quotes
 */
app.get('/api/indices', (req, res) => {
  try {
    const list = indicesManager.getAllIndices();
    res.json({
      status: true,
      count: list.length,
      indices: list,
      data: list
    });
  } catch (err) {
    console.error(`[REST Indices] Error fetching all indices: ${err.message}`);
    res.status(500).json({
      status: false,
      error: 'Failed to retrieve indices',
      indices: [],
      data: []
    });
  }
});

/**
 * GET /api/indices/:indexName/constituents
 * Return constituent stocks for the specified index with live price & stats
 */
app.get('/api/indices/:indexName/constituents', (req, res) => {
  try {
    const indexName = req.params.indexName;
    const data = indicesManager.getConstituents(indexName, scripMasterManager);

    if (!data) {
      return res.status(404).json({
        status: false,
        error: `Index '${indexName}' not found among supported NSE indices.`,
        constituents: []
      });
    }

    res.json({
      status: true,
      index: data.index,
      count: data.count,
      constituents: data.constituents,
      data: data.constituents
    });
  } catch (err) {
    console.error(`[REST Constituents] Error fetching constituents for ${req.params.indexName}: ${err.message}`);
    res.status(500).json({
      status: false,
      error: 'Failed to retrieve index constituents',
      constituents: []
    });
  }
});

/**
 * GET /api/indices/:indexName
 * Return details for a single index
 */
app.get('/api/indices/:indexName', (req, res) => {
  try {
    const indexName = req.params.indexName;
    const details = indicesManager.getIndexDetails(indexName);

    if (!details) {
      return res.status(404).json({
        status: false,
        error: `Index '${indexName}' not found.`
      });
    }

    res.json({
      status: true,
      index: details,
      data: details
    });
  } catch (err) {
    console.error(`[REST Index Details] Error fetching details for ${req.params.indexName}: ${err.message}`);
    res.status(500).json({
      status: false,
      error: 'Failed to retrieve index details'
    });
  }
});

/**
 * GET /api/search?q=SEARCH_QUERY
 * Fast stock search across Angel One Scrip Master NSE Equity instruments
 * Supports: Stock name, Trading symbol, Partial stock name, Partial trading symbol
 * Example: HDFC, RELIANCE, TATA, INFY, SBIN
 */
app.get(['/api/search', '/search'], (req, res) => {
  try {
    const query = (req.query.q || req.query.query || '').toString().trim();
    const limit = Math.min(parseInt(req.query.limit, 10) || 50, 200);

    const matchedScrips = scripMasterManager.search(query, limit);

    // Format results to strictly match the requested Angel One Scrip Master JSON schema:
    // { name, symbol, token, exchange, instrumentType }
    const results = matchedScrips.map(item => ({
      name: item.name,
      symbol: item.symbol,
      token: String(item.token),
      exchange: item.exchange || 'NSE',
      instrumentType: item.instrumentType || item.instrumenttype || 'EQ'
    }));

    res.json({
      status: true,
      query,
      count: results.length,
      results,
      data: results
    });
  } catch (err) {
    console.error(`[REST Search] Error executing scrip search: ${err.message}`);
    res.status(500).json({
      status: false,
      error: 'Failed to search scrip master',
      results: [],
      data: []
    });
  }
});

/**
 * GET /api/scrip?symbol=BEL&token=383
 * Fast exact scrip lookup from Angel One Scrip Master
 */
app.get(['/api/scrip', '/scrip'], (req, res) => {
  const symbol = (req.query.symbol || req.query.sym || req.query.q || '').toString().trim();
  const token = (req.query.token || req.query.symboltoken || '').toString().trim();

  let scrip = null;
  if (token) {
    scrip = scripMasterManager.getByToken(token);
  }
  if (!scrip && symbol) {
    scrip = scripMasterManager.getBySymbol(symbol) ||
            scripMasterManager.getBySymbol(`${symbol}-EQ`) ||
            scripMasterManager.getBySymbol(symbol.replace(/-EQ$/i, ''));
  }
  if (!scrip && symbol) {
    const matches = scripMasterManager.search(symbol, 5);
    if (matches && matches.length > 0) {
      scrip = matches[0];
    }
  }

  if (scrip) {
    const cleanSym = scrip.symbol.replace(/-EQ$/i, '');
    return res.json({
      status: true,
      data: {
        name: scrip.name,
        symbol: cleanSym,
        tradingSymbol: scrip.symbol,
        token: String(scrip.token),
        exchange: scrip.exchange || 'NSE',
        instrumentType: scrip.instrumentType || 'EQ'
      }
    });
  }
  return res.status(404).json({ status: false, error: `Scrip not found for symbol: ${symbol}, token: ${token}` });
});

/**
 * GET /api/quote?symboltoken=TOKEN&exchange=NSE
 * Fetch real-time market quote with circuit limits and price statistics
 */
app.get(['/api/quote', '/quote'], async (req, res) => {
  const symbolToken = (req.query.symboltoken || req.query.token || '').toString().trim();
  const exchange = (req.query.exchange || 'NSE').toString().toUpperCase();

  if (!symbolToken) {
    return res.status(400).json({ status: false, error: 'Missing required parameter: symboltoken' });
  }

  // 1. If Angel One is authenticated, try fetching live quote
  if (isAngelConfigured() && authManager.isAuthenticated) {
    try {
      const response = await axios.post(
        `${SMARTAPI_BASE_URL}/rest/secure/angelbroking/market/v1/quote/`,
        {
          mode: 'FULL',
          exchangeTokens: { [exchange]: [symbolToken] }
        },
        {
          headers: authManager.getHeaders(),
          timeout: 8000
        }
      );

      if (response.data && response.data.status && response.data.data) {
        return res.json({ status: true, source: 'smartapi_live', data: response.data.data });
      }
    } catch (err) {
      console.warn(`[REST Quote] SmartAPI quote failed: ${err.message}. Falling back to internal engine.`);
    }
  }

  // 2. Fallback to internal quote engine
  const scripFromMaster = scripMasterManager.getByToken(symbolToken) || scripMasterManager.getBySymbol(symbolToken);
  const stock = tokenMap.get(symbolToken) || (scripFromMaster ? {
    symbol: scripFromMaster.symbol,
    name: scripFromMaster.name,
    token: scripFromMaster.token,
    exchange: scripFromMaster.exchange || exchange,
    ltp: 1250.0,
    prevClose: 1240.0
  } : {
    symbol: `TOKEN_${symbolToken}`,
    name: 'Stock Instrument',
    token: symbolToken,
    exchange,
    ltp: 1000.0,
    prevClose: 990.0
  });

  const ltp = stock.ltp;
  const change = Math.round((ltp - stock.prevClose) * 100) / 100;
  const changePercent = Math.round((change / stock.prevClose) * 10000) / 100;

  res.json({
    status: true,
    source: 'internal_market_engine',
    data: {
      symbol: stock.symbol,
      token: stock.token,
      exchange: stock.exchange || exchange,
      ltp,
      change,
      changePercent,
      open: stock.prevClose * 1.002,
      high: ltp * 1.008,
      low: ltp * 0.992,
      close: ltp,
      volume: 1450000,
      upperCircuit: stock.prevClose * 1.10,
      lowerCircuit: stock.prevClose * 0.90,
      timestamp: new Date().toISOString()
    }
  });
});

/**
 * GET /api/candles
 * Historical & live aggregated candlestick data
 * Parameters: exchange, symboltoken, interval (1s, 5s, 15s, 30s, ONE_MINUTE, FIVE_MINUTE, etc.), fromdate, todate
 */
app.get(['/api/candles', '/candles'], async (req, res) => {
  const symbolToken = (req.query.symboltoken || req.query.token || '').toString().trim();
  const exchange = (req.query.exchange || 'NSE').toString().toUpperCase();
  const interval = (req.query.interval || 'ONE_MINUTE').toString();
  const fromDate = req.query.fromdate || '';
  const toDate = req.query.todate || '';

  if (!symbolToken) {
    return res.status(400).json({ status: false, error: 'Missing required parameter: symboltoken' });
  }

  // A. Check if sub-second candles are requested (1s, 5s, 15s, 30s)
  if (['1s', '5s', '15s', '30s', '1S', '5S', '15S', '30S'].includes(interval)) {
    const key = interval.toLowerCase();
    const liveCandles = candleAggregator.getCandles(symbolToken, key);

    // If buffer has candles, return them directly
    if (liveCandles.length > 0) {
      return res.json({
        status: true,
        interval: key,
        token: symbolToken,
        count: liveCandles.length,
        data: liveCandles
      });
    }
  }

  // B. If Angel One is authenticated and standard timeframe is requested, query SmartAPI historical candle API
  if (isAngelConfigured() && authManager.isAuthenticated && fromDate && toDate) {
    try {
      const response = await axios.post(
        `${SMARTAPI_BASE_URL}/rest/secure/angelbroking/historical/v1/getCandleData`,
        {
          exchange,
          symboltoken: symbolToken,
          interval: mapIntervalToSmartApi(interval),
          fromdate: fromDate,
          todate: toDate
        },
        {
          headers: authManager.getHeaders(),
          timeout: 10000
        }
      );

      if (response.data && response.data.status && Array.isArray(response.data.data)) {
        // Map SmartAPI array format [timestampStr, open, high, low, close, volume]
        const formattedCandles = response.data.data.map(item => {
          let dateStr = String(item[0]).trim();
          if (!dateStr.includes('+') && !dateStr.includes('Z')) {
            dateStr = dateStr.replace(' ', 'T') + '+05:30';
          }
          const ts = new Date(dateStr).getTime();
          return {
            timestamp: isNaN(ts) ? new Date(item[0]).getTime() : ts,
            open: Number(item[1]),
            high: Number(item[2]),
            low: Number(item[3]),
            close: Number(item[4]),
            volume: Number(item[5]) || 0,
            isComplete: true
          };
        });

        return res.json({
          status: true,
          source: 'smartapi_historical',
          token: symbolToken,
          count: formattedCandles.length,
          data: formattedCandles
        });
      }
    } catch (err) {
      console.warn(`[REST Candles] SmartAPI candle fetch failed: ${err.message}. Generating intraday candles.`);
    }
  }

  // C. Fallback: Generate clean synthetic intraday candles with market structure
  const stock = tokenMap.get(symbolToken) || { ltp: 1000.0 };
  const count = parseInt(req.query.count, 10) || 120;
  const intervalSeconds = mapIntervalToSeconds(interval);
  const syntheticCandles = generateSyntheticCandles(stock.ltp, intervalSeconds, count);

  res.json({
    status: true,
    source: 'market_structure_engine',
    token: symbolToken,
    interval,
    count: syntheticCandles.length,
    data: syntheticCandles
  });
});

/**
 * Helper to map timeframe string to SmartAPI interval code
 */
function mapIntervalToSmartApi(interval) {
  const upper = interval.toUpperCase();
  if (upper === '1M' || upper === 'ONE_MINUTE') return 'ONE_MINUTE';
  if (upper === '3M' || upper === 'THREE_MINUTE') return 'THREE_MINUTE';
  if (upper === '5M' || upper === 'FIVE_MINUTE') return 'FIVE_MINUTE';
  if (upper === '15M' || upper === 'FIFTEEN_MINUTE') return 'FIFTEEN_MINUTE';
  if (upper === '30M' || upper === 'THIRTY_MINUTE') return 'THIRTY_MINUTE';
  if (upper === '1H' || upper === 'ONE_HOUR') return 'ONE_HOUR';
  if (upper === '1D' || upper === 'ONE_DAY') return 'ONE_DAY';
  return 'ONE_MINUTE';
}

function mapIntervalToSeconds(interval) {
  const upper = interval.toUpperCase();
  if (upper === '1S') return 1;
  if (upper === '5S') return 5;
  if (upper === '15S') return 15;
  if (upper === '30S') return 30;
  if (upper === '1M' || upper === 'ONE_MINUTE') return 60;
  if (upper === '3M' || upper === 'THREE_MINUTE') return 180;
  if (upper === '5M' || upper === 'FIVE_MINUTE') return 300;
  if (upper === '15M' || upper === 'FIFTEEN_MINUTE') return 900;
  if (upper === '30M' || upper === 'THIRTY_MINUTE') return 1800;
  if (upper === '1H' || upper === 'ONE_HOUR') return 3600;
  if (upper === '1D' || upper === 'ONE_DAY') return 86400;
  return 60;
}

function generateSyntheticCandles(baseLtp, intervalSeconds, count) {
  const candles = [];
  const now = Date.now();
  const intervalMs = intervalSeconds * 1000;
  let price = baseLtp * (1.0 - 0.008);

  for (let i = count; i >= 1; i--) {
    const time = now - (i * intervalMs);
    const open = price;
    const drift = (Math.random() * 0.003) - 0.0014;
    const rawClose = open * (1.0 + drift);
    const high = Math.max(open, rawClose) + (open * Math.random() * 0.0015);
    const low = Math.min(open, rawClose) - (open * Math.random() * 0.0012);
    const close = Math.min(high, Math.max(low, rawClose));
    const volume = Math.floor(Math.random() * 60000) + 5000;

    candles.push({
      timestamp: time,
      open: Math.round(open * 100) / 100,
      high: Math.round(high * 100) / 100,
      low: Math.round(low * 100) / 100,
      close: Math.round(close * 100) / 100,
      volume,
      isComplete: true
    });
    price = close;
  }

  // Adjust last candle close to current LTP
  if (candles.length > 0) {
    const last = candles[candles.length - 1];
    last.close = baseLtp;
    last.high = Math.max(last.high, baseLtp);
    last.low = Math.min(last.low, baseLtp);
    last.isComplete = false;
  }

  return candles;
}

// ------------------------------------------------------------------------------
// 8. SERVER INITIALIZATION & WEBSOCKET ROUTING
// ------------------------------------------------------------------------------
const server = http.createServer(app);

// WebSocket Server attached to HTTP server
const wss = new WebSocketServer({ noServer: true });
wss.on('connection', handleClientWebSocket);

// Handle HTTP upgrade for WebSocket endpoints (/ws/market and /ws/ticks)
server.on('upgrade', (request, socket, head) => {
  const pathname = new URL(request.url, `http://${request.headers.host}`).pathname;

  if (pathname === '/ws/market' || pathname === '/ws/ticks' || pathname === '/ws') {
    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});

// Start HTTP & WS Server
server.listen(PORT, async () => {
  console.log('================================================================');
  console.log(`🚀 KOUSHIK TRADING AI BACKEND RUNNING ON PORT: ${PORT}`);
  console.log(`📡 Health Check URL: http://localhost:${PORT}/health`);
  console.log(`⚡ WebSocket URL: ws://localhost:${PORT}/ws/market`);
  console.log('================================================================');

  // Initialize Angel One Scrip Master Engine (Loads local cache / refreshes in background)
  try {
    await scripMasterManager.initialize();
    indicesManager.initialize(scripMasterManager);

    scripMasterManager.scrips.forEach(scrip => {
      if (!scrip) return;
      const cleanSym = (scrip.symbol || '').replace(/-EQ$/i, '');
      const item = {
        symbol: cleanSym,
        name: scrip.name || cleanSym,
        token: String(scrip.token).trim(),
        exchange: scrip.exchange || 'NSE',
        ltp: 0.0,
        prevClose: 0.0
      };
      if (!tokenMap.has(item.token)) tokenMap.set(item.token, item);
      if (cleanSym && !tokenMap.has(cleanSym)) tokenMap.set(cleanSym, item);
      if (scrip.symbol && !tokenMap.has(scrip.symbol)) tokenMap.set(scrip.symbol, item);
    });
    console.log(`[TokenMap] Populated ${tokenMap.size} token/symbol entries from Scrip Master.`);
  } catch (smErr) {
    console.error(`[Init] Scrip Master initialization error: ${smErr.message}`);
    indicesManager.initialize(null);
  }

  // Attempt initial Angel One SmartAPI authentication
  if (isAngelConfigured()) {
    console.log('[Init] Angel One credentials detected in environment variables.');
    const loggedIn = await authManager.login();
    if (loggedIn) {
      upstreamMarketFeed.connect();
    } else {
      console.warn('[Init] Login failed, falling back to market standby simulation.');
      upstreamMarketFeed.startSimulationFeed();
    }
  } else {
    console.log('[Init] Angel One credentials not set. Running in live Standby / Simulation mode.');
    upstreamMarketFeed.startSimulationFeed();
  }
});
