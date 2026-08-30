# KOUSHIK TRADING AI - BACKEND PROXY & MARKET DATA ENGINE

Production-ready Node.js & Express proxy server integrating **Angel One SmartAPI** with your **Koushik Trading AI** Android application.

---

## 🔒 Security Architecture

1. **Zero Credential Exposure:**
   All sensitive Angel One credentials (`API Key`, `Client ID`, `PIN`, `TOTP Secret`) reside strictly on this backend server. The Android mobile app **never** receives or stores credentials, tokens, or TOTP keys.
2. **Read-Only Market Data:**
   This backend is strictly configured for market data streaming, search, quotes, and candle generation. Order placement, execution, and funds management are completely disabled.
3. **Automated TOTP & Session Reconnection:**
   Automatically computes RFC 6238 TOTP codes, negotiates JWT sessions with Angel One, refreshes expired tokens, and reconnects smoothly.

---

## 📁 Project Structure

```
backend/
├── server.js          # Core Express server, SmartAPI client, 1-sec candle engine, WebSocket
├── package.json       # Node.js dependencies & scripts
├── .env.example       # Template of required environment variables
├── .gitignore         # Ignores .env and node_modules
├── render.yaml        # Render.com Blueprint configuration
└── README.md          # Complete documentation and setup guide
```

---

## ⚙️ Environment Variables

Add these environment variables to your **Render Dashboard** or local `.env` file:

| Variable | Description | Example |
|---|---|---|
| `PORT` | Server listening port | `3000` (Render assigns automatically) |
| `ANGEL_API_KEY` | SmartAPI App Key from Angel One developer portal | `YOUR_SMARTAPI_KEY` |
| `ANGEL_CLIENT_ID` | Your Angel One Trading Client Code | `K123456` |
| `ANGEL_PIN` | Your 4-digit Angel One Login PIN | `1234` |
| `ANGEL_TOTP_SECRET`| Base32 TOTP secret key for 2FA login | `JBSWY3DPEHPK3PXP` |
| `ANGEL_PUBLIC_IP` | Server / client public IP for Angel One compliance | `106.193.147.98` |
| `ANGEL_LOCAL_IP` | Local IP address | `192.168.1.1` |
| `ANGEL_MAC_ADDRESS` | Network MAC address | `fe80::216e:6507:4b90:3719` |
| `CORS_ORIGIN` | Allowed CORS origins for mobile access | `*` |

---

## 🚀 REST API Endpoints

### 1. Health Check
`GET /health`
```json
{
  "status": "ok",
  "service": "Koushik Trading AI Backend",
  "version": "1.0.0",
  "smartApi": {
    "configured": true,
    "authenticated": true,
    "sessionActive": true
  }
}
```

### 2. Search Stocks
`GET /api/search?q=RELIANCE`

### 3. Live Quote
`GET /api/quote?symboltoken=2885&exchange=NSE`

### 4. Historical & Sub-Second Candlesticks
`GET /api/candles?symboltoken=3045&interval=1s`
`GET /api/candles?symboltoken=3045&exchange=NSE&interval=ONE_MINUTE&fromdate=2024-08-01 09:15&todate=2024-08-01 15:30`

---

## ⚡ WebSocket Endpoint (`/ws/market`)

Connect via WebSocket to:
`wss://your-backend-app.onrender.com/ws/market`

### Subscribe to Stocks:
```json
{
  "action": "subscribe",
  "exchangeType": 1,
  "tokens": ["3045", "2885"]
}
```

### Unsubscribe:
```json
{
  "action": "unsubscribe",
  "tokens": ["3045"]
}
```

### Ping / Heartbeat:
```json
{
  "action": "ping"
}
```

### Real-Time 1-Second Candle Stream from Backend:
```json
{
  "type": "candle",
  "token": "3045",
  "interval": "1s",
  "candle": {
    "timestamp": 1725000000000,
    "open": 812.00,
    "high": 812.50,
    "low": 811.80,
    "close": 812.30,
    "volume": 1250,
    "isComplete": false
  }
}
```

---

## ☁️ Render Deployment Guide

1. Push your repository to **GitHub**.
2. Log in to [Render Dashboard](https://dashboard.render.com).
3. Click **New +** → **Web Service**.
4. Connect your GitHub repository.
5. Set the following settings:
   - **Root Directory:** `backend`
   - **Environment:** `Node`
   - **Build Command:** `npm install`
   - **Start Command:** `npm start`
6. Under **Environment Variables**, add the variables listed above.
7. Click **Create Web Service**.
