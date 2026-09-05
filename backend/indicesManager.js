/**
 * ==============================================================================
 * NSE INDICES & CONSTITUENTS DATA ENGINE
 * ==============================================================================
 * Provides high-reliability, updatable index constituent data for all major
 * NSE Sectoral, Broad Market, and Thematic indices.
 * 
 * Cross-references Angel One tokens & Scrip Master for live prices, quotes,
 * and intraday analytics.
 * ==============================================================================
 */

const fs = require('fs');
const path = require('path');
const axios = require('axios');

// Supported NSE Indices definitions with metadata & Angel One index tokens
const SUPPORTED_INDICES = [
  {
    id: 'nifty-50',
    symbol: 'NIFTY 50',
    name: 'NIFTY 50 INDEX',
    token: '99926000',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Flagship index tracking top 50 large-cap bluechip companies on NSE.',
    constituents: [
      'RELIANCE', 'HDFCBANK', 'ICICIBANK', 'INFY', 'TCS', 'ITC', 'LT', 'BHARTIARTL',
      'SBIN', 'AXISBANK', 'KOTAKBANK', 'BAJFINANCE', 'MARUTI', 'SUNPHARMA', 'TITAN',
      'TATASTEEL', 'TATAMOTORS', 'HINDUNILVR', 'NTPC', 'POWERGRID', 'COALINDIA',
      'ADANIENT', 'ADANIPORTS', 'M&M', 'ULTRACEMCO', 'GRASIM', 'JSWSTEEL', 'HINDALCO',
      'BPCL', 'ONGC', 'ASIANPAINT', 'HCLTECH', 'WIPRO', 'BAJAJFINSV', 'BAJAJ-AUTO',
      'NESTLEIND', 'TECHM', 'DRREDDY', 'CIPLA', 'APOLLOHOSP', 'DIVISLAB', 'EICHERMOT',
      'HEROMOTOCO', 'BRITANNIA', 'INDUSINDBK', 'TATACONSUM', 'SBILIFE', 'HDFCLIFE',
      'LTIM', 'BEL'
    ]
  },
  {
    id: 'nifty-next-50',
    symbol: 'NIFTY NEXT 50',
    name: 'NIFTY NEXT 50 INDEX',
    token: '99926013',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Represents 50 potential NIFTY 50 candidates (large-cap index 51-100).',
    constituents: [
      'ZOMATO', 'JIOFIN', 'VEDL', 'HAL', 'CHOLAFIN', 'TORNTPHARM', 'DLF', 'SIEMENS',
      'PIDILITIND', 'BANKBARODA', 'PNB', 'CANBK', 'TRENT', 'AMBUJACEM', 'SHRIRAMFIN',
      'MOTHERSON', 'GAIL', 'GODREJCP', 'DABUR', 'MARICO', 'BERGEPAINT', 'ICICIPRULI',
      'ICICIGI', 'SBICARD', 'HDFCAMC', 'ABB', 'BOSCHLTD', 'HAVELLS', 'INDIGO',
      'IRCTC', 'PFC', 'RECLTD', 'RVNL', 'IRFC', 'NHPC', 'IOC', 'BHEL', 'POLICYBZR',
      'NAUKRI', 'TATACOMM', 'TATAPOWER', 'JSWENERGY', 'ADANIPOWER', 'ADANIGREEN',
      'ATGL', 'CGPOWER', 'PERSISTENT', 'MUTHOOTFIN', 'COLPAL', 'POLYCAB'
    ]
  },
  {
    id: 'nifty-bank',
    symbol: 'NIFTY BANK',
    alias: 'BANKNIFTY',
    name: 'NIFTY BANK INDEX',
    token: '99926009',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Tracks the 12 most liquid and capitalized Indian banking stocks.',
    constituents: [
      'HDFCBANK', 'ICICIBANK', 'SBIN', 'AXISBANK', 'KOTAKBANK', 'INDUSINDBK',
      'BANKBARODA', 'PNB', 'AUBANK', 'FEDERALBNK', 'IDFCFIRSTB', 'BANDHANBNK'
    ]
  },
  {
    id: 'nifty-financial-services',
    symbol: 'NIFTY FINANCIAL SERVICES',
    alias: 'FINNIFTY',
    name: 'NIFTY FINANCIAL SERVICES INDEX',
    token: '99926037',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Tracks banks, NBFCs, insurance, asset management, and fintech firms.',
    constituents: [
      'HDFCBANK', 'ICICIBANK', 'SBIN', 'AXISBANK', 'KOTAKBANK', 'BAJFINANCE',
      'BAJAJFINSV', 'CHOLAFIN', 'HDFCLIFE', 'SBILIFE', 'MUTHOOTFIN', 'SHRIRAMFIN',
      'HDFCAMC', 'SBICARD', 'ICICIPRULI', 'ICICIGI', 'PFC', 'RECLTD', 'JIOFIN',
      'IDFCFIRSTB'
    ]
  },
  {
    id: 'nifty-it',
    symbol: 'NIFTY IT',
    name: 'NIFTY IT INDEX',
    token: '99926008',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Top Indian information technology & software services leaders.',
    constituents: [
      'TCS', 'INFY', 'HCLTECH', 'WIPRO', 'TECHM', 'LTIM', 'PERSISTENT', 'COFORGE',
      'LTTS', 'MPHASIS'
    ]
  },
  {
    id: 'nifty-auto',
    symbol: 'NIFTY AUTO',
    name: 'NIFTY AUTO INDEX',
    token: '99926029',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Automobile manufacturers, EV makers, and auto ancillary giants.',
    constituents: [
      'MARUTI', 'TATAMOTORS', 'M&M', 'BAJAJ-AUTO', 'EICHERMOT', 'HEROMOTOCO',
      'TVSMOTOR', 'BHARATFORG', 'MOTHERSON', 'MRF', 'ASHOKLEY', 'BOSCHLTD',
      'BALKRISIND', 'TIINDIA', 'APOLLOTYRE'
    ]
  },
  {
    id: 'nifty-fmcg',
    symbol: 'NIFTY FMCG',
    name: 'NIFTY FMCG INDEX',
    token: '99926021',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Fast-Moving Consumer Goods and household essentials companies.',
    constituents: [
      'HINDUNILVR', 'ITC', 'NESTLEIND', 'BRITANNIA', 'TATACONSUM', 'GODREJCP',
      'DABUR', 'MARICO', 'COLPAL', 'VBL', 'EMAMILTD', 'RADICO', 'UBL', 'PGHH',
      'BALRAMCHIN'
    ]
  },
  {
    id: 'nifty-pharma',
    symbol: 'NIFTY PHARMA',
    name: 'NIFTY PHARMA INDEX',
    token: '99926023',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Pharmaceutical manufacturers, healthcare research, and generic exporters.',
    constituents: [
      'SUNPHARMA', 'DRREDDY', 'CIPLA', 'DIVISLAB', 'ZYDUSLIFE', 'LUPIN',
      'TORNTPHARM', 'AUROPHARMA', 'MANKIND', 'ALKEM', 'IPCALAB', 'BIOCON',
      'GLENMARK', 'LAURUSLABS', 'ABBOTINDIA', 'GLAXO', 'SANOFI', 'NATCOPHARM',
      'JBCHEPHARM', 'GRANULES'
    ]
  },
  {
    id: 'nifty-metal',
    symbol: 'NIFTY METAL',
    name: 'NIFTY METAL INDEX',
    token: '99926030',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Ferrous, non-ferrous metals, mining, and steel manufacturing powerhouses.',
    constituents: [
      'TATASTEEL', 'JSWSTEEL', 'HINDALCO', 'VEDL', 'COALINDIA', 'JINDALSTEL',
      'NMDC', 'SAIL', 'NATIONALUM', 'HINDCOPPER', 'HINDZINC', 'RATNAMANI',
      'WELCORP', 'APLAPOLLO', 'JSL'
    ]
  },
  {
    id: 'nifty-realty',
    symbol: 'NIFTY REALTY',
    name: 'NIFTY REALTY INDEX',
    token: '99926018',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Residential, commercial real estate developers, and infrastructure firms.',
    constituents: [
      'DLF', 'MACROTECH', 'GODREJPROP', 'OBEROIRLTY', 'PHOENIXLTD', 'PRESTIGE',
      'BRIGADE', 'SOBHA', 'SUNTECK', 'MAHLIFE'
    ]
  },
  {
    id: 'nifty-media',
    symbol: 'NIFTY MEDIA',
    name: 'NIFTY MEDIA INDEX',
    token: '99926031',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Broadcasting, film exhibition, digital entertainment, and print media.',
    constituents: [
      'ZEEL', 'SUNTV', 'PVRINOX', 'NETWORK18', 'TV18BRDCST', 'DISHTV', 'NAZARA',
      'SAREGAMA', 'HATHWAY', 'NDTV'
    ]
  },
  {
    id: 'nifty-energy',
    symbol: 'NIFTY ENERGY',
    name: 'NIFTY ENERGY INDEX',
    token: '99926020',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Oil, gas, thermal, hydro, and renewable energy conglomerates.',
    constituents: [
      'RELIANCE', 'NTPC', 'POWERGRID', 'ONGC', 'COALINDIA', 'BPCL', 'IOC',
      'TATAPOWER', 'ADANIGREEN', 'ADANIPOWER', 'JSWENERGY', 'GAIL', 'OIL',
      'PETRONET', 'IGL'
    ]
  },
  {
    id: 'nifty-infra',
    symbol: 'NIFTY INFRA',
    name: 'NIFTY INFRA INDEX',
    token: '99926019',
    exchange: 'NSE',
    category: 'Thematic',
    description: 'Construction, telecom, ports, power transmission, and logistics leaders.',
    constituents: [
      'RELIANCE', 'LT', 'BHARTIARTL', 'NTPC', 'POWERGRID', 'ULTRACEMCO', 'GRASIM',
      'ADANIPORTS', 'ONGC', 'COALINDIA', 'TATAPOWER', 'GAIL', 'AMBUJACEM',
      'SIEMENS', 'ABB', 'DLF', 'CONCOR', 'INDIGO', 'IRCTC', 'BHEL', 'VOLTAS',
      'APOLLOHOSP', 'HAVELLS', 'GMRINFRA', 'JSWENERGY', 'ASHOKLEY', 'PETRONET',
      'TATACOMM', 'MRF', 'BALKRISIND'
    ]
  },
  {
    id: 'nifty-healthcare',
    symbol: 'NIFTY HEALTHCARE',
    name: 'NIFTY HEALTHCARE INDEX',
    token: '99926038',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Hospitals, diagnostic chains, biopharma, and medical devices.',
    constituents: [
      'SUNPHARMA', 'DRREDDY', 'CIPLA', 'APOLLOHOSP', 'DIVISLAB', 'MAXHEALTH',
      'MEDANTA', 'FORTIS', 'ZYDUSLIFE', 'LUPIN', 'TORNTPHARM', 'AUROPHARMA',
      'MANKIND', 'SYNGENE', 'LALPATHLAB', 'NH', 'ALKEM', 'BIOCON', 'IPCALAB',
      'LAURUSLABS'
    ]
  },
  {
    id: 'nifty-psu-bank',
    symbol: 'NIFTY PSU BANK',
    name: 'NIFTY PSU BANK INDEX',
    token: '99926025',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Government-owned public sector banking institutions in India.',
    constituents: [
      'SBIN', 'BANKBARODA', 'PNB', 'CANBK', 'UNIONBANK', 'INDIANB', 'BANKINDIA',
      'CENTRALBK', 'MAHABANK', 'UCOBANK', 'IOB', 'PSB'
    ]
  },
  {
    id: 'nifty-private-bank',
    symbol: 'NIFTY PRIVATE BANK',
    name: 'NIFTY PRIVATE BANK INDEX',
    token: '99926047',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Top Indian private sector commercial banks.',
    constituents: [
      'HDFCBANK', 'ICICIBANK', 'AXISBANK', 'KOTAKBANK', 'INDUSINDBK', 'FEDERALBNK',
      'IDFCFIRSTB', 'AUBANK', 'BANDHANBNK', 'CITYUNIONB'
    ]
  },
  {
    id: 'nifty-consumer-durables',
    symbol: 'NIFTY CONSUMER DURABLES',
    name: 'NIFTY CONSUMER DURABLES INDEX',
    token: '99926040',
    exchange: 'NSE',
    category: 'Sectoral',
    description: 'Electronics, home appliances, jewelry, and durable goods manufacturers.',
    constituents: [
      'TITAN', 'HAVELLS', 'DIXON', 'VOLTAS', 'BLUESTARCO', 'CROMPTON', 'WHIRLPOOL',
      'KAJARIACER', 'BATAINDIA', 'VGUARD', 'AMBER', 'RAJESHEXPO', 'CENTURYPLY',
      'SYMPHONY', 'ORIENTELEC'
    ]
  },
  {
    id: 'nifty-midcap-50',
    symbol: 'NIFTY MIDCAP 50',
    name: 'NIFTY MIDCAP 50 INDEX',
    token: '99926014',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Top 50 high-growth mid-cap equities listed on NSE.',
    constituents: [
      'POLYCAB', 'TRENT', 'PERSISTENT', 'COFORGE', 'FEDERALBNK', 'IDFCFIRSTB',
      'MAXHEALTH', 'BHARATFORG', 'TVSMOTOR', 'ASHOKLEY', 'CUMMINSIND', 'AUROPHARMA',
      'LUPIN', 'PIIND', 'ASTRAL', 'JUBLFOOD', 'DALBHARAT', 'OBEROIRLTY', 'ESCORTS',
      'MFSL', 'HINDPETRO', 'SUNDARMFIN', 'GMRINFRA', 'TATACOMM', 'LICHSGFIN',
      'GODREJPROP', 'DEEPAKNTR', 'IPCALAB', 'GLENMARK', 'COLPAL', 'MPHASIS',
      'APOLLOTYRE', 'BALKRISIND', 'VOLTAS', 'DIXON', 'BATAINDIA', 'BANDHANBNK',
      'MRF', 'NATIONALUM', 'NMDC', 'SAIL', 'CANBK', 'UNIONBANK', 'INDIANB',
      'UBL', 'RADICO', 'PAGEIND', 'ALKEM', 'BIOCON', 'CONCOR'
    ]
  },
  {
    id: 'nifty-midcap-100',
    symbol: 'NIFTY MIDCAP 100',
    name: 'NIFTY MIDCAP 100 INDEX',
    token: '99926011',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Comprehensive 100 mid-sized growth enterprises across all sectors.',
    constituents: [
      'POLYCAB', 'TRENT', 'PERSISTENT', 'COFORGE', 'FEDERALBNK', 'IDFCFIRSTB',
      'MAXHEALTH', 'BHARATFORG', 'TVSMOTOR', 'ASHOKLEY', 'CUMMINSIND', 'AUROPHARMA',
      'LUPIN', 'PIIND', 'ASTRAL', 'JUBLFOOD', 'DALBHARAT', 'OBEROIRLTY', 'ESCORTS',
      'MFSL', 'HINDPETRO', 'SUNDARMFIN', 'GMRINFRA', 'TATACOMM', 'LICHSGFIN',
      'GODREJPROP', 'DEEPAKNTR', 'IPCALAB', 'GLENMARK', 'COLPAL', 'MPHASIS',
      'APOLLOTYRE', 'BALKRISIND', 'VOLTAS', 'DIXON', 'BATAINDIA', 'BANDHANBNK',
      'MRF', 'NATIONALUM', 'NMDC', 'SAIL', 'CANBK', 'UNIONBANK', 'INDIANB',
      'UBL', 'RADICO', 'PAGEIND', 'ALKEM', 'BIOCON', 'CONCOR', 'LTTS', 'TATAELXSI',
      'PRESTIGE', 'PHOENIXLTD', 'FORTIS', 'MEDANTA', 'ZYDUSLIFE', 'MANKIND',
      'LAURUSLABS', 'SYNGENE', 'LALPATHLAB', 'NH', 'AUBANK', 'BANKINDIA', 'CENTRALBK',
      'MAHABANK', 'UCOBANK', 'IOB', 'PSB', 'CITYUNIONB', 'SUZLON', 'EXIDEIND',
      'CDSL', 'ANGELONE', 'BSE', 'HUDCO', 'NBCC', 'RITES', 'MAZDOCK', 'COCHINSHIP',
      'CESC', 'CASTROLIND', 'KEC', 'KARURVYSYA', 'MANAPPURAM', 'SONACOMS',
      'JBCHEPHARM', 'TATAINVEST', 'TEJASNET', 'CYIENT', 'BSESOFT', 'AMARAJABAT',
      'CAMS', 'TRIDENT', 'HFCL', 'PNBHOUSING', 'WELSPUNLIV', 'KAYNES', 'POONAWALLA',
      'BLS'
    ]
  },
  {
    id: 'nifty-smallcap-100',
    symbol: 'NIFTY SMALLCAP 100',
    name: 'NIFTY SMALLCAP 100 INDEX',
    token: '99926032',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Top 100 dynamic small-cap companies listed on National Stock Exchange.',
    constituents: [
      'SUZLON', 'EXIDEIND', 'CDSL', 'ANGELONE', 'BSE', 'HUDCO', 'NBCC', 'RITES',
      'MAZDOCK', 'COCHINSHIP', 'CESC', 'CASTROLIND', 'KEC', 'KARURVYSYA', 'MANAPPURAM',
      'SONACOMS', 'JBCHEPHARM', 'TATAINVEST', 'TEJASNET', 'CYIENT', 'AMARAJABAT',
      'CAMS', 'CENTRALBK', 'TRIDENT', 'HFCL', 'PNBHOUSING', 'WELSPUNLIV', 'KAYNES',
      'POONAWALLA', 'BLS', 'ENGINERSIN', 'IRB', 'IRCON', 'NCC', 'GRINFRA', 'PNCINFRA',
      'HBLPOWER', 'APARINDS', 'EIDPARRY', 'CENTURYTEX', 'CHAMBLFERT', 'GNFC', 'RCF',
      'DEEPAKFERT', 'GSFC', 'NATIONALUM', 'HINDCOPPER', 'HINDZINC', 'RATNAMANI',
      'WELCORP', 'APLAPOLLO', 'JSL', 'SUNTECK', 'MAHLIFE', 'SOBHA', 'SIGNATURE',
      'ZEEL', 'SUNTV', 'PVRINOX', 'NETWORK18', 'TV18BRDCST', 'DISHTV', 'NAZARA',
      'SAREGAMA', 'HATHWAY', 'NDTV', 'OIL', 'PETRONET', 'IGL', 'MGL', 'GUJGASLTD',
      'GSPL', 'CHENNPETRO', 'MRPL', 'AAVAS', 'HOMEFIRST', 'CANFINHOME', 'CREDITACC',
      'FIVESTAR', 'SPANDANA', 'EQUITASBNK', 'UJJIVANSFB', 'JSFB', 'SURYODAY',
      'FINPIPE', 'SUPREMEIND', 'ASTRAL', 'PRINCEPIPE', 'VGUARD', 'AMBER', 'SYMPHONY',
      'ORIENTELEC', 'RADICO', 'UBL', 'SULA', 'TI', 'VIPIND', 'SAFARI'
    ]
  },
  {
    id: 'nifty-100',
    symbol: 'NIFTY 100',
    name: 'NIFTY 100 INDEX',
    token: '99926012',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Consolidated index tracking the top 100 large-cap companies in India.',
    constituents: [
      'RELIANCE', 'HDFCBANK', 'ICICIBANK', 'INFY', 'TCS', 'ITC', 'LT', 'BHARTIARTL',
      'SBIN', 'AXISBANK', 'KOTAKBANK', 'BAJFINANCE', 'MARUTI', 'SUNPHARMA', 'TITAN',
      'TATASTEEL', 'TATAMOTORS', 'HINDUNILVR', 'NTPC', 'POWERGRID', 'COALINDIA',
      'ADANIENT', 'ADANIPORTS', 'M&M', 'ULTRACEMCO', 'GRASIM', 'JSWSTEEL', 'HINDALCO',
      'BPCL', 'ONGC', 'ASIANPAINT', 'HCLTECH', 'WIPRO', 'BAJAJFINSV', 'BAJAJ-AUTO',
      'NESTLEIND', 'TECHM', 'DRREDDY', 'CIPLA', 'APOLLOHOSP', 'DIVISLAB', 'EICHERMOT',
      'HEROMOTOCO', 'BRITANNIA', 'INDUSINDBK', 'TATACONSUM', 'SBILIFE', 'HDFCLIFE',
      'LTIM', 'BEL', 'ZOMATO', 'JIOFIN', 'VEDL', 'HAL', 'CHOLAFIN', 'TORNTPHARM',
      'DLF', 'SIEMENS', 'PIDILITIND', 'BANKBARODA', 'PNB', 'CANBK', 'TRENT',
      'AMBUJACEM', 'SHRIRAMFIN', 'MOTHERSON', 'GAIL', 'GODREJCP', 'DABUR', 'MARICO',
      'BERGEPAINT', 'ICICIPRULI', 'ICICIGI', 'SBICARD', 'HDFCAMC', 'ABB', 'BOSCHLTD',
      'HAVELLS', 'INDIGO', 'IRCTC', 'PFC', 'RECLTD', 'RVNL', 'IRFC', 'NHPC', 'IOC',
      'BHEL', 'POLICYBZR', 'NAUKRI', 'TATACOMM', 'TATAPOWER', 'JSWENERGY', 'ADANIPOWER',
      'ADANIGREEN', 'ATGL', 'CGPOWER', 'PERSISTENT', 'MUTHOOTFIN', 'COLPAL', 'POLYCAB'
    ]
  },
  {
    id: 'nifty-200',
    symbol: 'NIFTY 200',
    name: 'NIFTY 200 INDEX',
    token: '99926033',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Broad market index tracking top 200 large and mid-cap Indian companies.',
    constituents: [
      'RELIANCE', 'HDFCBANK', 'ICICIBANK', 'INFY', 'TCS', 'ITC', 'LT', 'BHARTIARTL',
      'SBIN', 'AXISBANK', 'KOTAKBANK', 'BAJFINANCE', 'MARUTI', 'SUNPHARMA', 'TITAN',
      'TATASTEEL', 'TATAMOTORS', 'HINDUNILVR', 'NTPC', 'POWERGRID', 'COALINDIA',
      'ADANIENT', 'ADANIPORTS', 'M&M', 'ULTRACEMCO', 'GRASIM', 'JSWSTEEL', 'HINDALCO',
      'BPCL', 'ONGC', 'ASIANPAINT', 'HCLTECH', 'WIPRO', 'BAJAJFINSV', 'BAJAJ-AUTO',
      'NESTLEIND', 'TECHM', 'DRREDDY', 'CIPLA', 'APOLLOHOSP', 'DIVISLAB', 'EICHERMOT',
      'HEROMOTOCO', 'BRITANNIA', 'INDUSINDBK', 'TATACONSUM', 'SBILIFE', 'HDFCLIFE',
      'LTIM', 'BEL', 'ZOMATO', 'JIOFIN', 'VEDL', 'HAL', 'CHOLAFIN', 'TORNTPHARM',
      'DLF', 'SIEMENS', 'PIDILITIND', 'BANKBARODA', 'PNB', 'CANBK', 'TRENT',
      'AMBUJACEM', 'SHRIRAMFIN', 'MOTHERSON', 'GAIL', 'GODREJCP', 'DABUR', 'MARICO',
      'BERGEPAINT', 'ICICIPRULI', 'ICICIGI', 'SBICARD', 'HDFCAMC', 'ABB', 'BOSCHLTD',
      'HAVELLS', 'INDIGO', 'IRCTC', 'PFC', 'RECLTD', 'RVNL', 'IRFC', 'NHPC', 'IOC',
      'BHEL', 'POLICYBZR', 'NAUKRI', 'TATACOMM', 'TATAPOWER', 'JSWENERGY', 'ADANIPOWER',
      'ADANIGREEN', 'ATGL', 'CGPOWER', 'PERSISTENT', 'MUTHOOTFIN', 'COLPAL', 'POLYCAB',
      'COFORGE', 'FEDERALBNK', 'IDFCFIRSTB', 'MAXHEALTH', 'BHARATFORG', 'TVSMOTOR',
      'ASHOKLEY', 'CUMMINSIND', 'AUROPHARMA', 'LUPIN', 'PIIND', 'ASTRAL', 'JUBLFOOD',
      'DALBHARAT', 'OBEROIRLTY', 'ESCORTS', 'MFSL', 'HINDPETRO', 'SUNDARMFIN',
      'GMRINFRA', 'LICHSGFIN', 'GODREJPROP', 'DEEPAKNTR', 'IPCALAB', 'GLENMARK',
      'MPHASIS', 'APOLLOTYRE', 'BALKRISIND', 'VOLTAS', 'DIXON', 'BATAINDIA',
      'BANDHANBNK', 'MRF', 'NATIONALUM', 'NMDC', 'SAIL', 'UNIONBANK', 'INDIANB',
      'UBL', 'RADICO', 'PAGEIND', 'ALKEM', 'BIOCON', 'CONCOR', 'LTTS', 'TATAELXSI',
      'PRESTIGE', 'PHOENIXLTD', 'FORTIS', 'MEDANTA', 'ZYDUSLIFE', 'MANKIND',
      'LAURUSLABS', 'SYNGENE', 'LALPATHLAB', 'NH', 'AUBANK', 'BANKINDIA', 'CENTRALBK',
      'MAHABANK', 'UCOBANK', 'IOB', 'PSB', 'CITYUNIONB', 'SUZLON', 'EXIDEIND',
      'CDSL', 'ANGELONE', 'BSE', 'HUDCO', 'NBCC', 'RITES', 'MAZDOCK', 'COCHINSHIP'
    ]
  },
  {
    id: 'nifty-500',
    symbol: 'NIFTY 500',
    name: 'NIFTY 500 INDEX',
    token: '99926004',
    exchange: 'NSE',
    category: 'Broad Market',
    description: 'Broadest market benchmark covering ~95% of total NSE market capitalization.',
    constituents: [
      'RELIANCE', 'HDFCBANK', 'ICICIBANK', 'INFY', 'TCS', 'ITC', 'LT', 'BHARTIARTL',
      'SBIN', 'AXISBANK', 'KOTAKBANK', 'BAJFINANCE', 'MARUTI', 'SUNPHARMA', 'TITAN',
      'TATASTEEL', 'TATAMOTORS', 'HINDUNILVR', 'NTPC', 'POWERGRID', 'COALINDIA',
      'ADANIENT', 'ADANIPORTS', 'M&M', 'ULTRACEMCO', 'GRASIM', 'JSWSTEEL', 'HINDALCO',
      'BPCL', 'ONGC', 'ASIANPAINT', 'HCLTECH', 'WIPRO', 'BAJAJFINSV', 'BAJAJ-AUTO',
      'NESTLEIND', 'TECHM', 'DRREDDY', 'CIPLA', 'APOLLOHOSP', 'DIVISLAB', 'EICHERMOT',
      'HEROMOTOCO', 'BRITANNIA', 'INDUSINDBK', 'TATACONSUM', 'SBILIFE', 'HDFCLIFE',
      'LTIM', 'BEL', 'ZOMATO', 'JIOFIN', 'VEDL', 'HAL', 'CHOLAFIN', 'TORNTPHARM',
      'DLF', 'SIEMENS', 'PIDILITIND', 'BANKBARODA', 'PNB', 'CANBK', 'TRENT',
      'AMBUJACEM', 'SHRIRAMFIN', 'MOTHERSON', 'GAIL', 'GODREJCP', 'DABUR', 'MARICO',
      'BERGEPAINT', 'ICICIPRULI', 'ICICIGI', 'SBICARD', 'HDFCAMC', 'ABB', 'BOSCHLTD',
      'HAVELLS', 'INDIGO', 'IRCTC', 'PFC', 'RECLTD', 'RVNL', 'IRFC', 'NHPC', 'IOC',
      'BHEL', 'POLICYBZR', 'NAUKRI', 'TATACOMM', 'TATAPOWER', 'JSWENERGY', 'ADANIPOWER',
      'ADANIGREEN', 'ATGL', 'CGPOWER', 'PERSISTENT', 'MUTHOOTFIN', 'COLPAL', 'POLYCAB',
      'COFORGE', 'FEDERALBNK', 'IDFCFIRSTB', 'MAXHEALTH', 'BHARATFORG', 'TVSMOTOR',
      'ASHOKLEY', 'CUMMINSIND', 'AUROPHARMA', 'LUPIN', 'PIIND', 'ASTRAL', 'JUBLFOOD',
      'DALBHARAT', 'OBEROIRLTY', 'ESCORTS', 'MFSL', 'HINDPETRO', 'SUNDARMFIN',
      'GMRINFRA', 'LICHSGFIN', 'GODREJPROP', 'DEEPAKNTR', 'IPCALAB', 'GLENMARK',
      'MPHASIS', 'APOLLOTYRE', 'BALKRISIND', 'VOLTAS', 'DIXON', 'BATAINDIA',
      'BANDHANBNK', 'MRF', 'NATIONALUM', 'NMDC', 'SAIL', 'UNIONBANK', 'INDIANB',
      'UBL', 'RADICO', 'PAGEIND', 'ALKEM', 'BIOCON', 'CONCOR', 'LTTS', 'TATAELXSI',
      'PRESTIGE', 'PHOENIXLTD', 'FORTIS', 'MEDANTA', 'ZYDUSLIFE', 'MANKIND',
      'LAURUSLABS', 'SYNGENE', 'LALPATHLAB', 'NH', 'AUBANK', 'BANKINDIA', 'CENTRALBK',
      'MAHABANK', 'UCOBANK', 'IOB', 'PSB', 'CITYUNIONB', 'SUZLON', 'EXIDEIND',
      'CDSL', 'ANGELONE', 'BSE', 'HUDCO', 'NBCC', 'RITES', 'MAZDOCK', 'COCHINSHIP',
      'CESC', 'CASTROLIND', 'KEC', 'KARURVYSYA', 'MANAPPURAM', 'SONACOMS',
      'JBCHEPHARM', 'TATAINVEST', 'TEJASNET', 'CYIENT', 'AMARAJABAT', 'CAMS',
      'TRIDENT', 'HFCL', 'PNBHOUSING', 'WELSPUNLIV', 'KAYNES', 'POONAWALLA',
      'BLS', 'ENGINERSIN', 'IRB', 'IRCON', 'NCC', 'GRINFRA', 'PNCINFRA',
      'HBLPOWER', 'APARINDS', 'EIDPARRY', 'CENTURYTEX', 'CHAMBLFERT', 'GNFC', 'RCF',
      'DEEPAKFERT', 'GSFC', 'HINDCOPPER', 'HINDZINC', 'RATNAMANI', 'WELCORP',
      'APLAPOLLO', 'JSL', 'SUNTECK', 'MAHLIFE', 'SOBHA', 'SIGNATURE', 'ZEEL',
      'SUNTV', 'PVRINOX', 'NETWORK18', 'TV18BRDCST', 'DISHTV', 'NAZARA', 'SAREGAMA',
      'HATHWAY', 'NDTV', 'OIL', 'PETRONET', 'IGL', 'MGL', 'GUJGASLTD', 'GSPL',
      'CHENNPETRO', 'MRPL', 'AAVAS', 'HOMEFIRST', 'CANFINHOME', 'CREDITACC',
      'FIVESTAR', 'SPANDANA', 'EQUITASBNK', 'UJJIVANSFB', 'JSFB', 'SURYODAY',
      'FINPIPE', 'SUPREMEIND', 'PRINCEPIPE', 'VGUARD', 'AMBER', 'SYMPHONY',
      'ORIENTELEC', 'SULA', 'TI', 'VIPIND', 'SAFARI'
    ]
  }
];

// Map of stock default market prices and corporate names for high fidelity presentation
const STOCK_INFO_MAP = {
  'RELIANCE': { name: 'Reliance Industries Ltd', ltp: 2980.40, prevClose: 2947.80, token: '2885', sector: 'Energy' },
  'HDFCBANK': { name: 'HDFC Bank Ltd', ltp: 1642.15, prevClose: 1650.60, token: '1333', sector: 'Banking' },
  'ICICIBANK': { name: 'ICICI Bank Ltd', ltp: 1198.80, prevClose: 1184.60, token: '4963', sector: 'Banking' },
  'INFY': { name: 'Infosys Ltd', ltp: 1795.50, prevClose: 1774.20, token: '1594', sector: 'IT' },
  'TCS': { name: 'Tata Consultancy Services Ltd', ltp: 4185.00, prevClose: 4139.80, token: '11536', sector: 'IT' },
  'ITC': { name: 'ITC Ltd', ltp: 468.90, prevClose: 470.10, token: '1660', sector: 'FMCG' },
  'LT': { name: 'Larsen & Toubro Ltd', ltp: 3620.00, prevClose: 3645.00, token: '11483', sector: 'Capital Goods' },
  'BHARTIARTL': { name: 'Bharti Airtel Ltd', ltp: 1485.00, prevClose: 1466.50, token: '10604', sector: 'Telecom' },
  'SBIN': { name: 'State Bank of India', ltp: 812.30, prevClose: 808.50, token: '3045', sector: 'Banking' },
  'AXISBANK': { name: 'Axis Bank Ltd', ltp: 1175.20, prevClose: 1168.90, token: '5900', sector: 'Banking' },
  'KOTAKBANK': { name: 'Kotak Mahindra Bank Ltd', ltp: 1785.40, prevClose: 1772.10, token: '1922', sector: 'Banking' },
  'BAJFINANCE': { name: 'Bajaj Finance Ltd', ltp: 6920.00, prevClose: 6880.00, token: '317', sector: 'Financial Services' },
  'MARUTI': { name: 'Maruti Suzuki India Ltd', ltp: 12450.00, prevClose: 12380.00, token: '10999', sector: 'Auto' },
  'SUNPHARMA': { name: 'Sun Pharmaceutical Industries Ltd', ltp: 1680.00, prevClose: 1672.00, token: '3351', sector: 'Pharma' },
  'TITAN': { name: 'Titan Company Ltd', ltp: 3580.00, prevClose: 3560.00, token: '3506', sector: 'Consumer Durables' },
  'TATASTEEL': { name: 'Tata Steel Ltd', ltp: 154.20, prevClose: 153.10, token: '3499', sector: 'Metals' },
  'TATAMOTORS': { name: 'Tata Motors Ltd', ltp: 984.60, prevClose: 997.00, token: '3456', sector: 'Auto' },
  'HINDUNILVR': { name: 'Hindustan Unilever Ltd', ltp: 2740.00, prevClose: 2725.00, token: '1394', sector: 'FMCG' },
  'NTPC': { name: 'NTPC Ltd', ltp: 412.50, prevClose: 409.80, token: '11630', sector: 'Energy' },
  'POWERGRID': { name: 'Power Grid Corp of India Ltd', ltp: 328.00, prevClose: 325.40, token: '14977', sector: 'Energy' },
  'COALINDIA': { name: 'Coal India Ltd', ltp: 512.00, prevClose: 508.20, token: '20374', sector: 'Metals & Mining' },
  'ADANIENT': { name: 'Adani Enterprises Ltd', ltp: 3040.00, prevClose: 3015.00, token: '25', sector: 'Metals & Mining' },
  'ADANIPORTS': { name: 'Adani Ports & SEZ Ltd', ltp: 1460.00, prevClose: 1445.00, token: '15083', sector: 'Infrastructure' },
  'M&M': { name: 'Mahindra & Mahindra Ltd', ltp: 2780.00, prevClose: 2750.00, token: '2031', sector: 'Auto' },
  'ULTRACEMCO': { name: 'UltraTech Cement Ltd', ltp: 11420.00, prevClose: 11350.00, token: '11532', sector: 'Cement' },
  'GRASIM': { name: 'Grasim Industries Ltd', ltp: 2680.00, prevClose: 2660.00, token: '1232', sector: 'Cement' },
  'JSWSTEEL': { name: 'JSW Steel Ltd', ltp: 945.00, prevClose: 938.00, token: '11723', sector: 'Metals' },
  'HINDALCO': { name: 'Hindalco Industries Ltd', ltp: 685.00, prevClose: 679.00, token: '1363', sector: 'Metals' },
  'BPCL': { name: 'Bharat Petroleum Corp Ltd', ltp: 345.00, prevClose: 342.00, token: '526', sector: 'Energy' },
  'ONGC': { name: 'Oil & Natural Gas Corp Ltd', ltp: 318.00, prevClose: 315.00, token: '2475', sector: 'Energy' },
  'ASIANPAINT': { name: 'Asian Paints Ltd', ltp: 3120.00, prevClose: 3090.00, token: '236', sector: 'Consumer Goods' },
  'HCLTECH': { name: 'HCL Technologies Ltd', ltp: 1780.00, prevClose: 1760.00, token: '7229', sector: 'IT' },
  'WIPRO': { name: 'Wipro Ltd', ltp: 524.00, prevClose: 518.00, token: '3787', sector: 'IT' },
  'BAJAJFINSV': { name: 'Bajaj Finserv Ltd', ltp: 1780.00, prevClose: 1765.00, token: '16675', sector: 'Financial Services' },
  'BAJAJ-AUTO': { name: 'Bajaj Auto Ltd', ltp: 10450.00, prevClose: 10380.00, token: '16669', sector: 'Auto' },
  'NESTLEIND': { name: 'Nestle India Ltd', ltp: 2480.00, prevClose: 2460.00, token: '17963', sector: 'FMCG' },
  'TECHM': { name: 'Tech Mahindra Ltd', ltp: 1560.00, prevClose: 1545.00, token: '13538', sector: 'IT' },
  'DRREDDY': { name: 'Dr Reddys Laboratories Ltd', ltp: 6720.00, prevClose: 6680.00, token: '881', sector: 'Pharma' },
  'CIPLA': { name: 'Cipla Ltd', ltp: 1580.00, prevClose: 1565.00, token: '694', sector: 'Pharma' },
  'APOLLOHOSP': { name: 'Apollo Hospitals Enterprise Ltd', ltp: 6940.00, prevClose: 6890.00, token: '157', sector: 'Healthcare' },
  'DIVISLAB': { name: 'Divis Laboratories Ltd', ltp: 4950.00, prevClose: 4910.00, token: '10940', sector: 'Pharma' },
  'EICHERMOT': { name: 'Eicher Motors Ltd', ltp: 4890.00, prevClose: 4850.00, token: '910', sector: 'Auto' },
  'HEROMOTOCO': { name: 'Hero MotoCorp Ltd', ltp: 5420.00, prevClose: 5380.00, token: '1348', sector: 'Auto' },
  'BRITANNIA': { name: 'Britannia Industries Ltd', ltp: 5890.00, prevClose: 5840.00, token: '547', sector: 'FMCG' },
  'INDUSINDBK': { name: 'IndusInd Bank Ltd', ltp: 1420.00, prevClose: 1408.00, token: '5258', sector: 'Banking' },
  'TATACONSUM': { name: 'Tata Consumer Products Ltd', ltp: 1180.00, prevClose: 1168.00, token: '3432', sector: 'FMCG' },
  'SBILIFE': { name: 'SBI Life Insurance Co Ltd', ltp: 1780.00, prevClose: 1760.00, token: '21808', sector: 'Financial Services' },
  'HDFCLIFE': { name: 'HDFC Life Insurance Co Ltd', ltp: 720.00, prevClose: 712.00, token: '467', sector: 'Financial Services' },
  'LTIM': { name: 'LTIMindtree Ltd', ltp: 5980.00, prevClose: 5920.00, token: '17818', sector: 'IT' },
  'BEL': { name: 'Bharat Electronics Ltd', ltp: 305.00, prevClose: 301.50, token: '383', sector: 'Defence' },
  'ZOMATO': { name: 'Zomato Ltd', ltp: 258.00, prevClose: 252.00, token: '5097', sector: 'Consumer Services' },
  'JIOFIN': { name: 'Jio Financial Services Ltd', ltp: 325.00, prevClose: 320.00, token: '18143', sector: 'Financial Services' },
  'VEDL': { name: 'Vedanta Ltd', ltp: 462.00, prevClose: 456.00, token: '3063', sector: 'Metals' },
  'HAL': { name: 'Hindustan Aeronautics Ltd', ltp: 4720.00, prevClose: 4680.00, token: '2303', sector: 'Defence' },
  'DLF': { name: 'DLF Ltd', ltp: 845.00, prevClose: 838.00, token: '14732', sector: 'Realty' },
  'PIDILITIND': { name: 'Pidilite Industries Ltd', ltp: 3180.00, prevClose: 3150.00, token: '2664', sector: 'Chemicals' },
  'BANKBARODA': { name: 'Bank of Baroda', ltp: 254.00, prevClose: 251.00, token: '4668', sector: 'Banking' },
  'PNB': { name: 'Punjab National Bank', ltp: 112.50, prevClose: 110.80, token: '10666', sector: 'Banking' },
  'CANBK': { name: 'Canara Bank', ltp: 108.00, prevClose: 106.50, token: '10794', sector: 'Banking' },
  'TRENT': { name: 'Trent Ltd', ltp: 7120.00, prevClose: 7040.00, token: '1964', sector: 'Retail' },
  'AMBUJACEM': { name: 'Ambuja Cements Ltd', ltp: 635.00, prevClose: 628.00, token: '1270', sector: 'Cement' },
  'SHRIRAMFIN': { name: 'Shriram Finance Ltd', ltp: 3250.00, prevClose: 3210.00, token: '4306', sector: 'Financial Services' },
  'GAIL': { name: 'GAIL (India) Ltd', ltp: 228.00, prevClose: 224.50, token: '4717', sector: 'Energy' },
  'GODREJCP': { name: 'Godrej Consumer Products Ltd', ltp: 1420.00, prevClose: 1405.00, token: '10099', sector: 'FMCG' },
  'DABUR': { name: 'Dabur India Ltd', ltp: 645.00, prevClose: 640.00, token: '772', sector: 'FMCG' },
  'MARICO': { name: 'Marico Ltd', ltp: 654.00, prevClose: 648.00, token: '4067', sector: 'FMCG' },
  'HAVELLS': { name: 'Havells India Ltd', ltp: 1980.00, prevClose: 1960.00, token: '9819', sector: 'Consumer Durables' },
  'INDIGO': { name: 'InterGlobe Aviation Ltd', ltp: 4680.00, prevClose: 4620.00, token: '11195', sector: 'Aviation' },
  'IRCTC': { name: 'Indian Railway Catering & Tourism Corp', ltp: 920.00, prevClose: 912.00, token: '13611', sector: 'Travel' },
  'PFC': { name: 'Power Finance Corporation Ltd', ltp: 512.00, prevClose: 506.00, token: '14299', sector: 'Financial Services' },
  'RECLTD': { name: 'REC Ltd', ltp: 585.00, prevClose: 578.00, token: '15355', sector: 'Financial Services' },
  'IOC': { name: 'Indian Oil Corporation Ltd', ltp: 172.00, prevClose: 170.00, token: '1624', sector: 'Energy' },
  'BHEL': { name: 'Bharat Heavy Electricals Ltd', ltp: 285.00, prevClose: 280.00, token: '438', sector: 'Capital Goods' },
  'TATAPOWER': { name: 'Tata Power Co Ltd', ltp: 435.00, prevClose: 428.00, token: '3426', sector: 'Energy' },
  'POLYCAB': { name: 'Polycab India Ltd', ltp: 6940.00, prevClose: 6880.00, token: '9590', sector: 'Capital Goods' },
  'PERSISTENT': { name: 'Persistent Systems Ltd', ltp: 5120.00, prevClose: 5060.00, token: '18365', sector: 'IT' },
  'COFORGE': { name: 'Coforge Ltd', ltp: 6780.00, prevClose: 6710.00, token: '11543', sector: 'IT' },
  'FEDERALBNK': { name: 'The Federal Bank Ltd', ltp: 198.00, prevClose: 195.00, token: '1023', sector: 'Banking' },
  'IDFCFIRSTB': { name: 'IDFC FIRST Bank Ltd', ltp: 76.50, prevClose: 75.80, token: '11184', sector: 'Banking' },
  'AUBANK': { name: 'AU Small Finance Bank Ltd', ltp: 685.00, prevClose: 678.00, token: '21238', sector: 'Banking' },
  'BANDHANBNK': { name: 'Bandhan Bank Ltd', ltp: 204.00, prevClose: 201.00, token: '2263', sector: 'Banking' },
  'UNIONBANK': { name: 'Union Bank of India', ltp: 128.00, prevClose: 126.00, token: '10606', sector: 'Banking' },
  'INDIANB': { name: 'Indian Bank', ltp: 545.00, prevClose: 538.00, token: '10815', sector: 'Banking' },
  'VOLTAS': { name: 'Voltas Ltd', ltp: 1780.00, prevClose: 1755.00, token: '3718', sector: 'Consumer Durables' },
  'DIXON': { name: 'Dixon Technologies Ltd', ltp: 12850.00, prevClose: 12690.00, token: '21690', sector: 'Consumer Durables' },
  'SUZLON': { name: 'Suzlon Energy Ltd', ltp: 78.50, prevClose: 76.80, token: '13528', sector: 'Energy' },
  'CDSL': { name: 'Central Depository Services Ltd', ltp: 1480.00, prevClose: 1450.00, token: '21174', sector: 'Financial Services' },
  'ANGELONE': { name: 'Angel One Ltd', ltp: 2680.00, prevClose: 2640.00, token: '87', sector: 'Financial Services' },
  'BSE': { name: 'BSE Ltd', ltp: 2940.00, prevClose: 2890.00, token: '19585', sector: 'Financial Services' },
  'HUDCO': { name: 'Housing & Urban Development Corp', ltp: 285.00, prevClose: 278.00, token: '20935', sector: 'Financial Services' },
  'NBCC': { name: 'NBCC (India) Ltd', ltp: 178.00, prevClose: 174.00, token: '18096', sector: 'Infrastructure' },
  'COCHINSHIP': { name: 'Cochin Shipyard Ltd', ltp: 1940.00, prevClose: 1895.00, token: '21469', sector: 'Shipbuilding' },
  'MAZDOCK': { name: 'Mazagon Dock Shipbuilders Ltd', ltp: 4350.00, prevClose: 4280.00, token: '2643', sector: 'Shipbuilding' }
};

class IndicesManager {
  constructor() {
    this.indices = SUPPORTED_INDICES;
    this.liveIndicesCache = new Map();
    this.liveStockCache = new Map();
    this.lastRefreshTime = null;
    this.refreshTimer = null;
  }

  initialize(scripMaster) {
    console.log(`[IndicesManager] Initializing ${this.indices.length} supported NSE indices...`);
    this.enrichWithScripMaster(scripMaster);
    this.scheduleAutoRefresh();
  }

  enrichWithScripMaster(scripMaster) {
    if (!scripMaster) return;

    for (const [sym, info] of Object.entries(STOCK_INFO_MAP)) {
      const fromMaster = scripMaster.getBySymbol(sym) || scripMaster.getBySymbol(`${sym}-EQ`);
      if (fromMaster) {
        info.token = String(fromMaster.token);
        if (fromMaster.name) info.name = fromMaster.name;
      }
    }
  }

  scheduleAutoRefresh() {
    // In live mode, indices are updated strictly via real-time WebSocket ticks & REST quote polling
  }

  /**
   * Update index state from real Angel One live tick
   */
  updateIndexFromTick(token, ltp, change = 0.0, changePercent = 0.0, high = ltp, low = ltp, prevClose = 0.0) {
    if (!token || ltp <= 0) return;

    const matchedIndices = this.indices.filter(idx => String(idx.token) === String(token));
    for (const idx of matchedIndices) {
      const existing = this.liveIndicesCache.get(idx.symbol.toUpperCase());
      const calculatedPrevClose = prevClose > 0 ? prevClose : (existing?.prevClose > 0 ? existing.prevClose : (change !== 0 ? (ltp - change) : ltp));
      const calculatedChange = change !== 0 ? change : (calculatedPrevClose > 0 ? Math.round((ltp - calculatedPrevClose) * 100) / 100 : 0.0);
      const calculatedPct = changePercent !== 0 ? changePercent : (calculatedPrevClose > 0 ? Math.round((calculatedChange / calculatedPrevClose) * 10000) / 100 : 0.0);
      const newHigh = existing && existing.high > 0 ? Math.max(existing.high, ltp, high) : Math.max(ltp, high);
      const newLow = existing && existing.low > 0 ? Math.min(existing.low, ltp, low) : Math.min(ltp, low);

      this.liveIndicesCache.set(idx.symbol.toUpperCase(), {
        id: idx.id,
        symbol: idx.symbol,
        alias: idx.alias || null,
        name: idx.name,
        token: idx.token,
        exchange: idx.exchange,
        category: idx.category,
        ltp,
        change: calculatedChange,
        changePercent: calculatedPct,
        high: newHigh,
        low: newLow,
        prevClose: calculatedPrevClose,
        constituentCount: idx.constituents.length,
        description: idx.description,
        isLive: true,
        status: 'LIVE',
        lastUpdated: new Date().toISOString()
      });
      console.log(`[IndicesManager Real Tick] Updated Index ${idx.symbol} (${idx.token}) -> LTP: ${ltp}, Change: ${calculatedChange} (${calculatedPct}%)`);
    }

    this.lastRefreshTime = new Date();
  }

  /**
   * Get all supported NSE indices with summary data
   */
  getAllIndices() {
    const list = [];
    for (const idx of this.indices) {
      const cached = this.liveIndicesCache.get(idx.symbol.toUpperCase());
      if (cached) {
        list.push(cached);
      } else {
        list.push({
          id: idx.id,
          symbol: idx.symbol,
          alias: idx.alias || null,
          name: idx.name,
          token: idx.token,
          exchange: idx.exchange,
          category: idx.category,
          ltp: 0.0,
          change: 0.0,
          changePercent: 0.0,
          high: 0.0,
          low: 0.0,
          prevClose: 0.0,
          constituentCount: idx.constituents.length,
          description: idx.description,
          isLive: false,
          status: 'AWAITING_LIVE_FEED',
          lastUpdated: null
        });
      }
    }
    return list;
  }

  /**
   * Find index by symbol, alias, or slug ID
   */
  findNormalIndex(nameOrId) {
    if (!nameOrId) return null;
    const clean = nameOrId.toString().trim().toUpperCase().replace(/_/g, ' ').replace(/-/g, ' ');
    const slug = nameOrId.toString().trim().toLowerCase().replace(/\s+/g, '-');

    return this.indices.find(idx => 
      idx.symbol.toUpperCase() === clean ||
      idx.id.toLowerCase() === slug ||
      (idx.alias && idx.alias.toUpperCase() === clean) ||
      idx.name.toUpperCase() === clean ||
      idx.symbol.toUpperCase().replace(/\s+/g, '') === clean.replace(/\s+/g, '')
    ) || null;
  }

  /**
   * Get index summary details
   */
  getIndexDetails(nameOrId) {
    const indexDef = this.findNormalIndex(nameOrId);
    if (!indexDef) return null;

    const cached = this.liveIndicesCache.get(indexDef.symbol.toUpperCase());
    return cached || {
      id: indexDef.id,
      symbol: indexDef.symbol,
      alias: indexDef.alias || null,
      name: indexDef.name,
      token: indexDef.token,
      exchange: indexDef.exchange,
      category: indexDef.category,
      ltp: 0.0,
      change: 0.0,
      changePercent: 0.0,
      high: 0.0,
      low: 0.0,
      prevClose: 0.0,
      constituentCount: indexDef.constituents.length,
      description: indexDef.description,
      isLive: false,
      status: 'AWAITING_LIVE_FEED',
      lastUpdated: null
    };
  }

  /**
   * Get all constituent stocks for a given index with live market data
   */
  getConstituents(nameOrId, scripMaster = null, tokenMap = null) {
    const indexDef = this.findNormalIndex(nameOrId);
    if (!indexDef) return null;

    const results = [];

    for (const sym of indexDef.constituents) {
      const cleanSym = sym.replace(/-EQ$/i, '').toUpperCase();
      const baseInfo = STOCK_INFO_MAP[cleanSym] || {
        name: `${cleanSym} LTD`,
        token: '1000',
        ltp: 100.0,
        prevClose: 100.0,
        sector: 'Diversified'
      };

      let resolvedToken = baseInfo.token;
      let resolvedName = baseInfo.name;

      if (scripMaster) {
        const scrip = scripMaster.getBySymbol(cleanSym) || scripMaster.getBySymbol(`${cleanSym}-EQ`);
        if (scrip) {
          resolvedToken = String(scrip.token);
          if (scrip.name) resolvedName = scrip.name;
        }
      }

      // Check if tokenMap or liveStockCache has current live price
      const liveStock = tokenMap ? (tokenMap.get(resolvedToken) || tokenMap.get(cleanSym)) : null;
      const ltp = liveStock && liveStock.ltp > 0 ? liveStock.ltp : (baseInfo.ltp || 0.0);
      const prevClose = liveStock && liveStock.prevClose > 0 ? liveStock.prevClose : (baseInfo.prevClose || ltp);
      const change = (ltp > 0 && prevClose > 0) ? Math.round((ltp - prevClose) * 100) / 100 : 0.0;
      const changePercent = (prevClose > 0 && change !== 0) ? Math.round((change / prevClose) * 10000) / 100 : 0.0;
      const high = liveStock && liveStock.high > 0 ? liveStock.high : (ltp > 0 ? Math.round(Math.max(ltp, prevClose) * 1.008 * 100) / 100 : 0.0);
      const low = liveStock && liveStock.low > 0 ? liveStock.low : (ltp > 0 ? Math.round(Math.min(ltp, prevClose) * 0.992 * 100) / 100 : 0.0);
      const isLive = Boolean(liveStock && liveStock.ltp > 0);

      results.push({
        symbol: cleanSym,
        tradingSymbol: `${cleanSym}-EQ`,
        name: resolvedName,
        token: resolvedToken,
        exchange: 'NSE',
        instrumentType: 'EQ',
        ltp: ltp,
        change: change,
        changePercent: changePercent,
        high: high,
        low: low,
        open: prevClose,
        previousClose: prevClose,
        volume: liveStock && liveStock.volume ? liveStock.volume : 0,
        sector: baseInfo.sector,
        isLive: isLive,
        lastUpdated: liveStock && liveStock.timestamp ? liveStock.timestamp : Date.now()
      });
    }

    return {
      index: {
        id: indexDef.id,
        symbol: indexDef.symbol,
        alias: indexDef.alias || null,
        name: indexDef.name,
        token: indexDef.token,
        category: indexDef.category
      },
      count: results.length,
      constituents: results
    };
  }
}

const indicesManager = new IndicesManager();

module.exports = {
  indicesManager,
  SUPPORTED_INDICES,
  STOCK_INFO_MAP
};
