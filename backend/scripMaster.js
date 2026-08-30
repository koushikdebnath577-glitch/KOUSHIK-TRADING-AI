/**
 * ==============================================================================
 * ANGEL ONE SMARTAPI SCRIP MASTER MANAGER & SEARCH ENGINE
 * ==============================================================================
 * Downloads, filters, caches, and indexes the official Angel One Scrip Master
 * for high-speed stock search across NSE Equity instruments.
 * ==============================================================================
 */

const fs = require('fs');
const path = require('path');
const axios = require('axios');

const PRIMARY_SCRIP_MASTER_URL = 'https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json';
const FALLBACK_SCRIP_MASTER_URL = 'https://margincalculator.angelone.in/OpenAPI_File/files/OpenAPIScripMaster.json';

const CACHE_FILE_PATH = path.join(__dirname, 'scrip_master_cache.json');
const REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24 hours

// Well-known corporate names map for enhanced search, relevance, and presentation
const KNOWN_COMPANY_NAMES = {
  'RELIANCE': 'Reliance Industries Ltd',
  'HDFCBANK': 'HDFC Bank Ltd',
  'TCS': 'Tata Consultancy Services Ltd',
  'INFY': 'Infosys Ltd',
  'ICICIBANK': 'ICICI Bank Ltd',
  'SBIN': 'State Bank of India',
  'TATAMOTORS': 'Tata Motors Ltd',
  'TATASTEEL': 'Tata Steel Ltd',
  'TATACONSUM': 'Tata Consumer Products Ltd',
  'TATAPOWER': 'Tata Power Co Ltd',
  'ITC': 'ITC Ltd',
  'BHARTIARTL': 'Bharti Airtel Ltd',
  'LT': 'Larsen & Toubro Ltd',
  'KOTAKBANK': 'Kotak Mahindra Bank Ltd',
  'AXISBANK': 'Axis Bank Ltd',
  'BAJFINANCE': 'Bajaj Finance Ltd',
  'BAJAJFINSV': 'Bajaj Finserv Ltd',
  'BAJAJ-AUTO': 'Bajaj Auto Ltd',
  'MARUTI': 'Maruti Suzuki India Ltd',
  'SUNPHARMA': 'Sun Pharmaceutical Industries Ltd',
  'TITAN': 'Titan Company Ltd',
  'ASIANPAINT': 'Asian Paints Ltd',
  'HCLTECH': 'HCL Technologies Ltd',
  'WIPRO': 'Wipro Ltd',
  'NTPC': 'NTPC Ltd',
  'POWERGRID': 'Power Grid Corporation of India Ltd',
  'COALINDIA': 'Coal India Ltd',
  'ADANIENT': 'Adani Enterprises Ltd',
  'ADANIPORTS': 'Adani Ports & SEZ Ltd',
  'ADANIPOWER': 'Adani Power Ltd',
  'ADANIGREEN': 'Adani Green Energy Ltd',
  'HINDUNILVR': 'Hindustan Unilever Ltd',
  'ONGC': 'Oil & Natural Gas Corp Ltd',
  'M&M': 'Mahindra & Mahindra Ltd',
  'ULTRACEMCO': 'UltraTech Cement Ltd',
  'GRASIM': 'Grasim Industries Ltd',
  'JSWSTEEL': 'JSW Steel Ltd',
  'HINDALCO': 'Hindalco Industries Ltd',
  'BPCL': 'Bharat Petroleum Corp Ltd',
  'IOC': 'Indian Oil Corporation Ltd',
  'VEDL': 'Vedanta Ltd',
  'ZOMATO': 'Zomato Ltd',
  'JIOFIN': 'Jio Financial Services Ltd',
  'HDFCLIFE': 'HDFC Life Insurance Co Ltd',
  'HDFCAMC': 'HDFC Asset Management Co Ltd',
  'SBILIFE': 'SBI Life Insurance Co Ltd',
  'SBICARD': 'SBI Cards and Payment Services Ltd'
};

// Built-in foundational Indian Stock Directory (High-Reliability Fallback)
const DEFAULT_FALLBACK_STOCKS = [
  { name: 'NIFTY 50 INDEX', symbol: 'NIFTY 50', token: '99926000', exchange: 'NSE', instrumentType: 'AMXIDX' },
  { name: 'NIFTY BANK INDEX', symbol: 'BANKNIFTY', token: '99926009', exchange: 'NSE', instrumentType: 'AMXIDX' },
  { name: 'NIFTY FINANCIAL SERVICES', symbol: 'FINNIFTY', token: '99926037', exchange: 'NSE', instrumentType: 'AMXIDX' },
  { name: 'INDIA VIX VOLATILITY INDEX', symbol: 'INDIA VIX', token: '99926017', exchange: 'NSE', instrumentType: 'AMXIDX' },
  { name: 'Reliance Industries Ltd', symbol: 'RELIANCE-EQ', token: '2885', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'HDFC Bank Ltd', symbol: 'HDFCBANK-EQ', token: '1333', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Tata Consultancy Services Ltd', symbol: 'TCS-EQ', token: '11536', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Infosys Ltd', symbol: 'INFY-EQ', token: '1594', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'ICICI Bank Ltd', symbol: 'ICICIBANK-EQ', token: '4963', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'State Bank of India', symbol: 'SBIN-EQ', token: '3045', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Tata Motors Ltd', symbol: 'TATAMOTORS-EQ', token: '3456', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Tata Steel Ltd', symbol: 'TATASTEEL-EQ', token: '3499', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Tata Consumer Products Ltd', symbol: 'TATACONSUM-EQ', token: '3432', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Tata Power Co Ltd', symbol: 'TATAPOWER-EQ', token: '3426', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'ITC Ltd', symbol: 'ITC-EQ', token: '1660', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Bharti Airtel Ltd', symbol: 'BHARTIARTL-EQ', token: '10604', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Larsen & Toubro Ltd', symbol: 'LT-EQ', token: '11483', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Kotak Mahindra Bank Ltd', symbol: 'KOTAKBANK-EQ', token: '1922', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Axis Bank Ltd', symbol: 'AXISBANK-EQ', token: '5900', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Bajaj Finance Ltd', symbol: 'BAJFINANCE-EQ', token: '317', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Bajaj Finserv Ltd', symbol: 'BAJAJFINSV-EQ', token: '16675', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Bajaj Auto Ltd', symbol: 'BAJAJ-AUTO-EQ', token: '16669', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Maruti Suzuki India Ltd', symbol: 'MARUTI-EQ', token: '10999', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Sun Pharmaceutical Industries Ltd', symbol: 'SUNPHARMA-EQ', token: '3351', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Titan Company Ltd', symbol: 'TITAN-EQ', token: '3506', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Asian Paints Ltd', symbol: 'ASIANPAINT-EQ', token: '236', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'HCL Technologies Ltd', symbol: 'HCLTECH-EQ', token: '7229', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Wipro Ltd', symbol: 'WIPRO-EQ', token: '3787', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'NTPC Ltd', symbol: 'NTPC-EQ', token: '11630', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Power Grid Corporation of India Ltd', symbol: 'POWERGRID-EQ', token: '14977', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Coal India Ltd', symbol: 'COALINDIA-EQ', token: '20374', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Adani Enterprises Ltd', symbol: 'ADANIENT-EQ', token: '25', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Adani Ports & SEZ Ltd', symbol: 'ADANIPORTS-EQ', token: '15083', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Adani Power Ltd', symbol: 'ADANIPOWER-EQ', token: '17388', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Adani Green Energy Ltd', symbol: 'ADANIGREEN-EQ', token: '3563', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Hindustan Unilever Ltd', symbol: 'HINDUNILVR-EQ', token: '1394', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Oil & Natural Gas Corp Ltd', symbol: 'ONGC-EQ', token: '2475', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Mahindra & Mahindra Ltd', symbol: 'M&M-EQ', token: '2031', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'UltraTech Cement Ltd', symbol: 'ULTRACEMCO-EQ', token: '11532', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Grasim Industries Ltd', symbol: 'GRASIM-EQ', token: '1232', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'JSW Steel Ltd', symbol: 'JSWSTEEL-EQ', token: '11723', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Hindalco Industries Ltd', symbol: 'HINDALCO-EQ', token: '1363', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Bharat Petroleum Corp Ltd', symbol: 'BPCL-EQ', token: '526', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Indian Oil Corporation Ltd', symbol: 'IOC-EQ', token: '1624', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Vedanta Ltd', symbol: 'VEDL-EQ', token: '3063', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Zomato Ltd', symbol: 'ZOMATO-EQ', token: '5097', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'Jio Financial Services Ltd', symbol: 'JIOFIN-EQ', token: '18143', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'HDFC Life Insurance Co Ltd', symbol: 'HDFCLIFE-EQ', token: '467', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'HDFC Asset Management Co Ltd', symbol: 'HDFCAMC-EQ', token: '4244', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'SBI Life Insurance Co Ltd', symbol: 'SBILIFE-EQ', token: '21808', exchange: 'NSE', instrumentType: 'EQ' },
  { name: 'SBI Cards and Payment Services Ltd', symbol: 'SBICARD-EQ', token: '17971', exchange: 'NSE', instrumentType: 'EQ' }
];

class ScripMasterManager {
  constructor() {
    this.scrips = [...DEFAULT_FALLBACK_STOCKS];
    this.tokenIndex = new Map();
    this.symbolIndex = new Map();
    this.lastUpdated = null;
    this.isFetching = false;
    this.totalInstruments = this.scrips.length;
    this.refreshTimer = null;

    this.rebuildIndices();
  }

  rebuildIndices() {
    this.tokenIndex.clear();
    this.symbolIndex.clear();

    for (const scrip of this.scrips) {
      if (scrip.token) {
        this.tokenIndex.set(String(scrip.token).trim(), scrip);
      }
      if (scrip.symbol) {
        const rawSym = String(scrip.symbol).trim().toUpperCase();
        this.symbolIndex.set(rawSym, scrip);
        const cleanSymbol = rawSym.replace(/-EQ$/i, '');
        if (!this.symbolIndex.has(cleanSymbol)) {
          this.symbolIndex.set(cleanSymbol, scrip);
        }
      }
    }
    this.totalInstruments = this.scrips.length;
  }

  /**
   * Initializes the Scrip Master:
   * 1. Loads local cached file if available.
   * 2. Triggers background download if cache is missing or older than 24h.
   * 3. Sets 24-hour refresh timer.
   */
  async initialize() {
    console.log('[ScripMaster] Initializing Angel One Scrip Master Engine...');

    let cacheLoaded = false;
    if (fs.existsSync(CACHE_FILE_PATH)) {
      try {
        const raw = fs.readFileSync(CACHE_FILE_PATH, 'utf8');
        const parsed = JSON.parse(raw);
        if (parsed && Array.isArray(parsed.scrips) && parsed.scrips.length > 0) {
          this.scrips = parsed.scrips;
          this.lastUpdated = parsed.lastUpdated ? new Date(parsed.lastUpdated) : new Date();
          this.rebuildIndices();
          cacheLoaded = true;
          console.log(`[ScripMaster] Loaded ${this.scrips.length} cached NSE equities (Last updated: ${this.lastUpdated.toISOString()})`);
        }
      } catch (err) {
        console.warn(`[ScripMaster] Failed to read local cache: ${err.message}`);
      }
    }

    const cacheAge = this.lastUpdated ? (Date.now() - this.lastUpdated.getTime()) : Infinity;
    if (!cacheLoaded || cacheAge > REFRESH_INTERVAL_MS) {
      console.log(`[ScripMaster] Cache is ${cacheLoaded ? 'stale (>24h)' : 'missing'}. Fetching official Scrip Master in background...`);
      this.fetchScripMaster().catch(err => {
        console.warn(`[ScripMaster] Initial background fetch notice: ${err.message}`);
      });
    }

    // Schedule 24-hour automatic refresh
    this.schedule24HourRefresh();
  }

  schedule24HourRefresh() {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
    this.refreshTimer = setInterval(() => {
      console.log('[ScripMaster] 24-hour timer triggered: Refreshing Scrip Master from Angel One...');
      this.fetchScripMaster().catch(err => {
        console.error(`[ScripMaster] Scheduled 24h refresh error: ${err.message}`);
      });
    }, REFRESH_INTERVAL_MS);

    if (this.refreshTimer && this.refreshTimer.unref) {
      this.refreshTimer.unref();
    }
  }

  /**
   * Downloads official OpenAPIScripMaster.json from Angel One SmartAPI,
   * filters for NSE Equity instruments, and updates in-memory + local disk cache.
   */
  async fetchScripMaster() {
    if (this.isFetching) return;
    this.isFetching = true;

    try {
      console.log('[ScripMaster] Downloading official Scrip Master from Angel One SmartAPI...');
      let response = null;

      try {
        response = await axios.get(PRIMARY_SCRIP_MASTER_URL, {
          timeout: 45000,
          headers: {
            'Accept': 'application/json, text/plain, */*',
            'User-Agent': 'KoushikTradingAI/1.0'
          },
          maxContentLength: 100 * 1024 * 1024
        });
      } catch (primaryErr) {
        console.warn(`[ScripMaster] Primary URL failed (${primaryErr.message}). Trying fallback URL...`);
        response = await axios.get(FALLBACK_SCRIP_MASTER_URL, {
          timeout: 45000,
          headers: {
            'Accept': 'application/json, text/plain, */*',
            'User-Agent': 'KoushikTradingAI/1.0'
          },
          maxContentLength: 100 * 1024 * 1024
        });
      }

      if (response && Array.isArray(response.data)) {
        const rawList = response.data;
        console.log(`[ScripMaster] Downloaded ${rawList.length} total Angel One instruments. Filtering NSE Equities...`);

        const filtered = [];
        const seenTokens = new Set();

        // 1. Always retain key market indices
        for (const def of DEFAULT_FALLBACK_STOCKS) {
          if (def.instrumentType === 'AMXIDX') {
            filtered.push(def);
            seenTokens.add(def.token);
          }
        }

        // 2. Filter for NSE Tradable Equities
        for (const item of rawList) {
          if (!item || !item.token || !item.symbol) continue;

          const exch = (item.exch_seg || '').trim().toUpperCase();
          const sym = (item.symbol || '').trim().toUpperCase();
          const instType = (item.instrumenttype || '').trim().toUpperCase();
          const expiry = (item.expiry || '').trim();

          // Must be NSE exchange
          if (exch !== 'NSE') continue;

          // Exclude derivatives (Futures & Options)
          const isDerivative = expiry !== '' ||
            instType.includes('OPT') ||
            instType.includes('FUT') ||
            /\d+(CE|PE)$/i.test(sym) ||
            /-FUT$/i.test(sym);

          if (isDerivative) continue;

          // Must be equity
          const isEquity = sym.endsWith('-EQ') || instType === 'EQ' || instType === '';
          if (!isEquity) continue;

          const tokenStr = String(item.token).trim();

          if (!seenTokens.has(tokenStr)) {
            seenTokens.add(tokenStr);

            const baseSymbol = sym.replace(/-EQ$/i, '').toUpperCase();
            let resolvedName = KNOWN_COMPANY_NAMES[baseSymbol];

            if (!resolvedName) {
              const rawName = (item.name || baseSymbol).trim();
              if (rawName && rawName !== baseSymbol && rawName.length > baseSymbol.length) {
                resolvedName = rawName;
              } else {
                resolvedName = `${baseSymbol} LTD`;
              }
            }

            filtered.push({
              name: resolvedName,
              symbol: sym.endsWith('-EQ') ? sym : `${sym}-EQ`,
              token: tokenStr,
              exchange: 'NSE',
              instrumentType: instType || 'EQ'
            });
          }
        }

        if (filtered.length > 50) {
          this.scrips = filtered;
          this.lastUpdated = new Date();
          this.rebuildIndices();

          // Save compact cache to disk
          try {
            const cachePayload = {
              lastUpdated: this.lastUpdated.toISOString(),
              count: this.scrips.length,
              scrips: this.scrips
            };
            fs.writeFileSync(CACHE_FILE_PATH, JSON.stringify(cachePayload), 'utf8');
            console.log(`[ScripMaster] Successfully cached ${this.scrips.length} NSE equities to disk.`);
          } catch (writeErr) {
            console.warn(`[ScripMaster] Could not write cache file to disk: ${writeErr.message}`);
          }
        } else {
          console.warn(`[ScripMaster] Filtered list has too few items (${filtered.length}), retaining existing directory.`);
        }
      } else {
        throw new Error('Invalid Scrip Master response format');
      }
    } catch (err) {
      console.error(`[ScripMaster] Scrip Master download failed: ${err.message}. Retaining active in-memory catalog.`);
    } finally {
      this.isFetching = false;
    }
  }

  /**
   * Fast In-Memory Search Engine
   * Supports: Stock Name, Trading Symbol, Partial Name, Partial Symbol
   * E.g. HDFC, RELIANCE, TATA, INFY, SBIN
   */
  search(query, limit = 50) {
    const rawQuery = (query || '').toString().trim();
    if (!rawQuery) {
      return this.scrips.slice(0, limit);
    }

    const q = rawQuery.toUpperCase();
    const qClean = q.replace(/[^A-Z0-9]/g, '');

    const scoredMatches = [];

    for (const item of this.scrips) {
      const sym = item.symbol.toUpperCase();
      const cleanSym = sym.replace(/-EQ$/i, '');
      const symClean = sym.replace(/[^A-Z0-9]/g, '');
      const cleanSymAlpha = cleanSym.replace(/[^A-Z0-9]/g, '');
      const name = item.name.toUpperCase();
      const nameClean = name.replace(/[^A-Z0-9]/g, '');
      const token = item.token;

      let score = 0;

      // 1. Exact matches on symbol or token
      if (cleanSym === q || cleanSymAlpha === qClean || sym === q) {
        score = 1000;
      } else if (sym === `${q}-EQ` || symClean === `${qClean}EQ`) {
        score = 980;
      } else if (token === q) {
        score = 950;
      }
      // 2. Base Symbol starts with query (e.g. HDFC -> HDFCBANK)
      else if (cleanSym.startsWith(q) || cleanSymAlpha.startsWith(qClean)) {
        score = 850 - (cleanSym.length - q.length) * 2;
      }
      // 3. Name starts with query (e.g. TATA -> TATA MOTORS, RELIANCE -> Reliance Industries)
      else if (name.startsWith(q)) {
        score = 800 - (name.length - q.length);
      }
      // 4. Word boundary match in name (e.g. "TATA" matches "TATA CONSULTANCY SERVICES", "BANK" matches "HDFC BANK")
      else if (name.includes(` ${q}`) || name.includes(`-${q}`) || name.includes(`(${q}`)) {
        score = 700;
      }
      // 5. Symbol contains substring
      else if (sym.includes(q) || symClean.includes(qClean)) {
        score = 500;
      }
      // 6. Name contains substring
      else if (name.includes(q) || nameClean.includes(qClean)) {
        score = 350;
      }

      // Bonus boost for known large caps and indices so top stocks rank above penny stocks
      if (score > 0) {
        if (KNOWN_COMPANY_NAMES[cleanSym]) {
          score += 40;
        }
        if (item.instrumentType === 'AMXIDX') {
          score += 50;
        }
        scoredMatches.push({ item, score });
      }
    }

    // Sort by relevance score descending, then symbol length ascending
    scoredMatches.sort((a, b) => {
      if (b.score !== a.score) {
        return b.score - a.score;
      }
      return a.item.symbol.length - b.item.symbol.length;
    });

    return scoredMatches.slice(0, limit).map(m => m.item);
  }

  getByToken(token) {
    return this.tokenIndex.get(String(token).trim()) || null;
  }

  getBySymbol(symbol) {
    return this.symbolIndex.get(String(symbol).trim().toUpperCase()) || null;
  }

  getStatus() {
    return {
      totalInstruments: this.totalInstruments,
      lastUpdated: this.lastUpdated ? this.lastUpdated.toISOString() : null,
      isFetching: this.isFetching,
      cachedOnDisk: fs.existsSync(CACHE_FILE_PATH)
    };
  }
}

const scripMasterManager = new ScripMasterManager();

module.exports = {
  scripMasterManager,
  DEFAULT_FALLBACK_STOCKS
};
