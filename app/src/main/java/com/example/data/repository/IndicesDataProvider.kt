package com.example.data.repository

import com.example.data.model.IndexItem
import com.example.data.model.StockSymbol
import kotlin.random.Random

/**
 * Provides static definitions, constituent stock data, and fallback datasets
 * for all 23 supported NSE Indices.
 */
object IndicesDataProvider {

    val DEFAULT_INDICES: List<IndexItem> = listOf(
        IndexItem(
            id = "nifty-50",
            symbol = "NIFTY 50",
            name = "NIFTY 50 INDEX",
            token = "99926000",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 50,
            description = "Flagship index tracking top 50 large-cap bluechip companies on NSE."
        ),
        IndexItem(
            id = "nifty-next-50",
            symbol = "NIFTY NEXT 50",
            name = "NIFTY NEXT 50 INDEX",
            token = "99926013",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 50,
            description = "Represents 50 potential NIFTY 50 candidates (large-cap index 51-100)."
        ),
        IndexItem(
            id = "nifty-bank",
            symbol = "NIFTY BANK",
            alias = "BANKNIFTY",
            name = "NIFTY BANK INDEX",
            token = "99926009",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 12,
            description = "Tracks the 12 most liquid and capitalized Indian banking stocks."
        ),
        IndexItem(
            id = "nifty-financial-services",
            symbol = "NIFTY FINANCIAL SERVICES",
            alias = "FINNIFTY",
            name = "NIFTY FINANCIAL SERVICES INDEX",
            token = "99926037",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 20,
            description = "Tracks banks, NBFCs, insurance, asset management, and fintech firms."
        ),
        IndexItem(
            id = "nifty-it",
            symbol = "NIFTY IT",
            name = "NIFTY IT INDEX",
            token = "99926008",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 10,
            description = "Top Indian information technology & software services leaders."
        ),
        IndexItem(
            id = "nifty-auto",
            symbol = "NIFTY AUTO",
            name = "NIFTY AUTO INDEX",
            token = "99926029",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 15,
            description = "Automobile manufacturers, EV makers, and auto ancillary giants."
        ),
        IndexItem(
            id = "nifty-fmcg",
            symbol = "NIFTY FMCG",
            name = "NIFTY FMCG INDEX",
            token = "99926021",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 15,
            description = "Fast-Moving Consumer Goods and household essentials companies."
        ),
        IndexItem(
            id = "nifty-pharma",
            symbol = "NIFTY PHARMA",
            name = "NIFTY PHARMA INDEX",
            token = "99926023",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 20,
            description = "Pharmaceutical manufacturers, healthcare research, and generic exporters."
        ),
        IndexItem(
            id = "nifty-metal",
            symbol = "NIFTY METAL",
            name = "NIFTY METAL INDEX",
            token = "99926030",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 15,
            description = "Ferrous, non-ferrous metals, mining, and steel manufacturing powerhouses."
        ),
        IndexItem(
            id = "nifty-realty",
            symbol = "NIFTY REALTY",
            name = "NIFTY REALTY INDEX",
            token = "99926018",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 10,
            description = "Residential, commercial real estate developers, and infrastructure firms."
        ),
        IndexItem(
            id = "nifty-media",
            symbol = "NIFTY MEDIA",
            name = "NIFTY MEDIA INDEX",
            token = "99926031",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 10,
            description = "Broadcasting, film exhibition, digital entertainment, and print media."
        ),
        IndexItem(
            id = "nifty-energy",
            symbol = "NIFTY ENERGY",
            name = "NIFTY ENERGY INDEX",
            token = "99926020",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 15,
            description = "Oil, gas, thermal, hydro, and renewable energy conglomerates."
        ),
        IndexItem(
            id = "nifty-infra",
            symbol = "NIFTY INFRA",
            name = "NIFTY INFRA INDEX",
            token = "99926019",
            category = "Thematic",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 30,
            description = "Construction, telecom, ports, power transmission, and logistics leaders."
        ),
        IndexItem(
            id = "nifty-healthcare",
            symbol = "NIFTY HEALTHCARE",
            name = "NIFTY HEALTHCARE INDEX",
            token = "99926038",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 20,
            description = "Hospitals, diagnostic chains, biopharma, and medical devices."
        ),
        IndexItem(
            id = "nifty-psu-bank",
            symbol = "NIFTY PSU BANK",
            name = "NIFTY PSU BANK INDEX",
            token = "99926025",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 12,
            description = "Government-owned public sector banking institutions in India."
        ),
        IndexItem(
            id = "nifty-private-bank",
            symbol = "NIFTY PRIVATE BANK",
            name = "NIFTY PRIVATE BANK INDEX",
            token = "99926047",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 10,
            description = "Top Indian private sector commercial banks."
        ),
        IndexItem(
            id = "nifty-consumer-durables",
            symbol = "NIFTY CONSUMER DURABLES",
            name = "NIFTY CONSUMER DURABLES INDEX",
            token = "99926040",
            category = "Sectoral",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 15,
            description = "Electronics, home appliances, jewelry, and durable goods manufacturers."
        ),
        IndexItem(
            id = "nifty-midcap-50",
            symbol = "NIFTY MIDCAP 50",
            name = "NIFTY MIDCAP 50 INDEX",
            token = "99926014",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 50,
            description = "Top 50 high-growth mid-cap equities listed on NSE."
        ),
        IndexItem(
            id = "nifty-midcap-100",
            symbol = "NIFTY MIDCAP 100",
            name = "NIFTY MIDCAP 100 INDEX",
            token = "99926011",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 100,
            description = "Comprehensive 100 mid-sized growth enterprises across all sectors."
        ),
        IndexItem(
            id = "nifty-smallcap-100",
            symbol = "NIFTY SMALLCAP 100",
            name = "NIFTY SMALLCAP 100 INDEX",
            token = "99926032",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 100,
            description = "Top 100 dynamic small-cap companies listed on National Stock Exchange."
        ),
        IndexItem(
            id = "nifty-100",
            symbol = "NIFTY 100",
            name = "NIFTY 100 INDEX",
            token = "99926012",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 100,
            description = "Consolidated index tracking the top 100 large-cap companies in India."
        ),
        IndexItem(
            id = "nifty-200",
            symbol = "NIFTY 200",
            name = "NIFTY 200 INDEX",
            token = "99926033",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 200,
            description = "Broad market index tracking top 200 large and mid-cap Indian companies."
        ),
        IndexItem(
            id = "nifty-500",
            symbol = "NIFTY 500",
            name = "NIFTY 500 INDEX",
            token = "99926004",
            category = "Broad Market",
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            high = 0.0,
            low = 0.0,
            prevClose = 0.0,
            constituentCount = 500,
            description = "Broadest market benchmark covering ~95% of total NSE market capitalization."
        )
    )

    private fun createStock(symbol: String, name: String, token: String, exchange: String = "NSE"): StockSymbol {
        return StockSymbol(
            symbol = symbol,
            name = name,
            token = token,
            exchange = exchange,
            ltp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            open = 0.0,
            high = 0.0,
            low = 0.0,
            close = 0.0,
            volume = 0L,
            previousClose = 0.0
        )
    }

    // Comprehensive stock details master map
    private val STOCK_MASTER: Map<String, StockSymbol> = mapOf(
        "RELIANCE" to createStock("RELIANCE", "Reliance Industries Ltd", "2885"),
        "HDFCBANK" to createStock("HDFCBANK", "HDFC Bank Ltd", "1333"),
        "ICICIBANK" to createStock("ICICIBANK", "ICICI Bank Ltd", "4963"),
        "INFY" to createStock("INFY", "Infosys Ltd", "1594"),
        "TCS" to createStock("TCS", "Tata Consultancy Services", "11536"),
        "ITC" to createStock("ITC", "ITC Ltd", "1660"),
        "LT" to createStock("LT", "Larsen & Toubro Ltd", "11483"),
        "BHARTIARTL" to createStock("BHARTIARTL", "Bharti Airtel Ltd", "10604"),
        "SBIN" to createStock("SBIN", "State Bank of India", "3045"),
        "AXISBANK" to createStock("AXISBANK", "Axis Bank Ltd", "5900"),
        "KOTAKBANK" to createStock("KOTAKBANK", "Kotak Mahindra Bank Ltd", "1922"),
        "BAJFINANCE" to createStock("BAJFINANCE", "Bajaj Finance Ltd", "317"),
        "MARUTI" to createStock("MARUTI", "Maruti Suzuki India Ltd", "10999"),
        "SUNPHARMA" to createStock("SUNPHARMA", "Sun Pharmaceutical Ltd", "3351"),
        "TITAN" to createStock("TITAN", "Titan Company Ltd", "3506"),
        "TATASTEEL" to createStock("TATASTEEL", "Tata Steel Ltd", "3499"),
        "TATAMOTORS" to createStock("TATAMOTORS", "Tata Motors Ltd", "3456"),
        "HINDUNILVR" to createStock("HINDUNILVR", "Hindustan Unilever Ltd", "1394"),
        "NTPC" to createStock("NTPC", "NTPC Ltd", "11630"),
        "POWERGRID" to createStock("POWERGRID", "Power Grid Corp of India", "14977"),
        "COALINDIA" to createStock("COALINDIA", "Coal India Ltd", "20374"),
        "ADANIENT" to createStock("ADANIENT", "Adani Enterprises Ltd", "25"),
        "ADANIPORTS" to createStock("ADANIPORTS", "Adani Ports & SEZ Ltd", "15083"),
        "M&M" to createStock("M&M", "Mahindra & Mahindra Ltd", "2031"),
        "ULTRACEMCO" to createStock("ULTRACEMCO", "UltraTech Cement Ltd", "11532"),
        "GRASIM" to createStock("GRASIM", "Grasim Industries Ltd", "1232"),
        "JSWSTEEL" to createStock("JSWSTEEL", "JSW Steel Ltd", "11723"),
        "HINDALCO" to createStock("HINDALCO", "Hindalco Industries Ltd", "1363"),
        "BPCL" to createStock("BPCL", "Bharat Petroleum Corp Ltd", "526"),
        "ONGC" to createStock("ONGC", "Oil & Natural Gas Corp Ltd", "2475"),
        "ASIANPAINT" to createStock("ASIANPAINT", "Asian Paints Ltd", "236"),
        "HCLTECH" to createStock("HCLTECH", "HCL Technologies Ltd", "7229"),
        "WIPRO" to createStock("WIPRO", "Wipro Ltd", "3787"),
        "BAJAJFINSV" to createStock("BAJAJFINSV", "Bajaj Finserv Ltd", "16675"),
        "BAJAJ-AUTO" to createStock("BAJAJ-AUTO", "Bajaj Auto Ltd", "16669"),
        "NESTLEIND" to createStock("NESTLEIND", "Nestle India Ltd", "17963"),
        "TECHM" to createStock("TECHM", "Tech Mahindra Ltd", "13538"),
        "DRREDDY" to createStock("DRREDDY", "Dr Reddys Laboratories Ltd", "881"),
        "CIPLA" to createStock("CIPLA", "Cipla Ltd", "694"),
        "APOLLOHOSP" to createStock("APOLLOHOSP", "Apollo Hospitals Enterprise", "157"),
        "DIVISLAB" to createStock("DIVISLAB", "Divis Laboratories Ltd", "10940"),
        "EICHERMOT" to createStock("EICHERMOT", "Eicher Motors Ltd", "910"),
        "HEROMOTOCO" to createStock("HEROMOTOCO", "Hero MotoCorp Ltd", "1348"),
        "BRITANNIA" to createStock("BRITANNIA", "Britannia Industries Ltd", "547"),
        "INDUSINDBK" to createStock("INDUSINDBK", "IndusInd Bank Ltd", "5258"),
        "TATACONSUM" to createStock("TATACONSUM", "Tata Consumer Products Ltd", "3432"),
        "SBILIFE" to createStock("SBILIFE", "SBI Life Insurance Co Ltd", "21808"),
        "HDFCLIFE" to createStock("HDFCLIFE", "HDFC Life Insurance Co Ltd", "467"),
        "LTIM" to createStock("LTIM", "LTIMindtree Ltd", "17818"),
        "BEL" to createStock("BEL", "Bharat Electronics Ltd", "383"),
        "ZOMATO" to createStock("ZOMATO", "Zomato Ltd", "5097"),
        "JIOFIN" to createStock("JIOFIN", "Jio Financial Services Ltd", "18143"),
        "VEDL" to createStock("VEDL", "Vedanta Ltd", "3063"),
        "HAL" to createStock("HAL", "Hindustan Aeronautics Ltd", "2303"),
        "DLF" to createStock("DLF", "DLF Ltd", "14732"),
        "PIDILITIND" to createStock("PIDILITIND", "Pidilite Industries Ltd", "2664"),
        "BANKBARODA" to createStock("BANKBARODA", "Bank of Baroda", "4668"),
        "PNB" to createStock("PNB", "Punjab National Bank", "10666"),
        "CANBK" to createStock("CANBK", "Canara Bank", "10794"),
        "TRENT" to createStock("TRENT", "Trent Ltd", "1964"),
        "AMBUJACEM" to createStock("AMBUJACEM", "Ambuja Cements Ltd", "1270"),
        "SHRIRAMFIN" to createStock("SHRIRAMFIN", "Shriram Finance Ltd", "4306"),
        "GAIL" to createStock("GAIL", "GAIL (India) Ltd", "4717"),
        "GODREJCP" to createStock("GODREJCP", "Godrej Consumer Products", "10099"),
        "DABUR" to createStock("DABUR", "Dabur India Ltd", "772"),
        "MARICO" to createStock("MARICO", "Marico Ltd", "4067"),
        "HAVELLS" to createStock("HAVELLS", "Havells India Ltd", "9819"),
        "INDIGO" to createStock("INDIGO", "InterGlobe Aviation Ltd", "11195"),
        "IRCTC" to createStock("IRCTC", "Indian Railway Catering & Tourism", "13611"),
        "PFC" to createStock("PFC", "Power Finance Corporation Ltd", "14299"),
        "RECLTD" to createStock("RECLTD", "REC Ltd", "15355"),
        "IOC" to createStock("IOC", "Indian Oil Corporation Ltd", "1624"),
        "BHEL" to createStock("BHEL", "Bharat Heavy Electricals Ltd", "438"),
        "TATAPOWER" to createStock("TATAPOWER", "Tata Power Co Ltd", "3426"),
        "POLYCAB" to createStock("POLYCAB", "Polycab India Ltd", "9590"),
        "PERSISTENT" to createStock("PERSISTENT", "Persistent Systems Ltd", "18365"),
        "COFORGE" to createStock("COFORGE", "Coforge Ltd", "11543"),
        "FEDERALBNK" to createStock("FEDERALBNK", "The Federal Bank Ltd", "1023"),
        "IDFCFIRSTB" to createStock("IDFCFIRSTB", "IDFC FIRST Bank Ltd", "11184"),
        "AUBANK" to createStock("AUBANK", "AU Small Finance Bank Ltd", "21238"),
        "BANDHANBNK" to createStock("BANDHANBNK", "Bandhan Bank Ltd", "2263"),
        "UNIONBANK" to createStock("UNIONBANK", "Union Bank of India", "10606"),
        "INDIANB" to createStock("INDIANB", "Indian Bank", "10815"),
        "VOLTAS" to createStock("VOLTAS", "Voltas Ltd", "3718"),
        "DIXON" to createStock("DIXON", "Dixon Technologies Ltd", "21690"),
        "SUZLON" to createStock("SUZLON", "Suzlon Energy Ltd", "13528"),
        "CDSL" to createStock("CDSL", "Central Depository Services", "21174"),
        "ANGELONE" to createStock("ANGELONE", "Angel One Ltd", "87"),
        "BSE" to createStock("BSE", "BSE Ltd", "19585"),
        "HUDCO" to createStock("HUDCO", "Housing & Urban Development", "20935"),
        "NBCC" to createStock("NBCC", "NBCC (India) Ltd", "18096"),
        "COCHINSHIP" to createStock("COCHINSHIP", "Cochin Shipyard Ltd", "21469"),
        "MAZDOCK" to createStock("MAZDOCK", "Mazagon Dock Shipbuilders", "2643")
    )

    // Index -> List of constituent symbols
    private val CONSTITUENTS_MAP: Map<String, List<String>> = mapOf(
        "nifty-50" to listOf(
            "RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS", "ITC", "LT", "BHARTIARTL",
            "SBIN", "AXISBANK", "KOTAKBANK", "BAJFINANCE", "MARUTI", "SUNPHARMA", "TITAN",
            "TATASTEEL", "TATAMOTORS", "HINDUNILVR", "NTPC", "POWERGRID", "COALINDIA",
            "ADANIENT", "ADANIPORTS", "M&M", "ULTRACEMCO", "GRASIM", "JSWSTEEL", "HINDALCO",
            "BPCL", "ONGC", "ASIANPAINT", "HCLTECH", "WIPRO", "BAJAJFINSV", "BAJAJ-AUTO",
            "NESTLEIND", "TECHM", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB", "EICHERMOT",
            "HEROMOTOCO", "BRITANNIA", "INDUSINDBK", "TATACONSUM", "SBILIFE", "HDFCLIFE",
            "LTIM", "BEL"
        ),
        "nifty-next-50" to listOf(
            "ZOMATO", "JIOFIN", "VEDL", "HAL", "CHOLAFIN", "TORNTPHARM", "DLF", "SIEMENS",
            "PIDILITIND", "BANKBARODA", "PNB", "CANBK", "TRENT", "AMBUJACEM", "SHRIRAMFIN",
            "MOTHERSON", "GAIL", "GODREJCP", "DABUR", "MARICO", "BERGEPAINT", "ICICIPRULI",
            "ICICIGI", "SBICARD", "HDFCAMC", "ABB", "BOSCHLTD", "HAVELLS", "INDIGO",
            "IRCTC", "PFC", "RECLTD", "RVNL", "IRFC", "NHPC", "IOC", "BHEL", "POLICYBZR",
            "NAUKRI", "TATACOMM", "TATAPOWER", "JSWENERGY", "ADANIPOWER", "ADANIGREEN",
            "ATGL", "CGPOWER", "PERSISTENT", "MUTHOOTFIN", "COLPAL", "POLYCAB"
        ),
        "nifty-bank" to listOf(
            "HDFCBANK", "ICICIBANK", "SBIN", "AXISBANK", "KOTAKBANK", "INDUSINDBK",
            "BANKBARODA", "PNB", "AUBANK", "FEDERALBNK", "IDFCFIRSTB", "BANDHANBNK"
        ),
        "nifty-financial-services" to listOf(
            "HDFCBANK", "ICICIBANK", "SBIN", "AXISBANK", "KOTAKBANK", "BAJFINANCE",
            "BAJAJFINSV", "CHOLAFIN", "HDFCLIFE", "SBILIFE", "MUTHOOTFIN", "SHRIRAMFIN",
            "HDFCAMC", "SBICARD", "ICICIPRULI", "ICICIGI", "PFC", "RECLTD", "JIOFIN",
            "IDFCFIRSTB"
        ),
        "nifty-it" to listOf(
            "TCS", "INFY", "HCLTECH", "WIPRO", "TECHM", "LTIM", "PERSISTENT", "COFORGE",
            "LTTS", "MPHASIS"
        ),
        "nifty-auto" to listOf(
            "MARUTI", "TATAMOTORS", "M&M", "BAJAJ-AUTO", "EICHERMOT", "HEROMOTOCO",
            "TVSMOTOR", "BHARATFORG", "MOTHERSON", "MRF", "ASHOKLEY", "BOSCHLTD",
            "BALKRISIND", "TIINDIA", "APOLLOTYRE"
        ),
        "nifty-fmcg" to listOf(
            "HINDUNILVR", "ITC", "NESTLEIND", "BRITANNIA", "TATACONSUM", "GODREJCP",
            "DABUR", "MARICO", "COLPAL", "VBL", "EMAMILTD", "RADICO", "UBL", "PGHH",
            "BALRAMCHIN"
        ),
        "nifty-pharma" to listOf(
            "SUNPHARMA", "DRREDDY", "CIPLA", "DIVISLAB", "ZYDUSLIFE", "LUPIN",
            "TORNTPHARM", "AUROPHARMA", "MANKIND", "ALKEM", "IPCALAB", "BIOCON",
            "GLENMARK", "LAURUSLABS", "ABBOTINDIA", "GLAXO", "SANOFI", "NATCOPHARM",
            "JBCHEPHARM", "GRANULES"
        ),
        "nifty-metal" to listOf(
            "TATASTEEL", "JSWSTEEL", "HINDALCO", "VEDL", "COALINDIA", "JINDALSTEL",
            "NMDC", "SAIL", "NATIONALUM", "HINDCOPPER", "HINDZINC", "RATNAMANI",
            "WELCORP", "APLAPOLLO", "JSL"
        ),
        "nifty-realty" to listOf(
            "DLF", "MACROTECH", "GODREJPROP", "OBEROIRLTY", "PHOENIXLTD", "PRESTIGE",
            "BRIGADE", "SOBHA", "SUNTECK", "MAHLIFE"
        ),
        "nifty-media" to listOf(
            "ZEEL", "SUNTV", "PVRINOX", "NETWORK18", "TV18BRDCST", "DISHTV", "NAZARA",
            "SAREGAMA", "HATHWAY", "NDTV"
        ),
        "nifty-energy" to listOf(
            "RELIANCE", "NTPC", "POWERGRID", "ONGC", "COALINDIA", "BPCL", "IOC",
            "TATAPOWER", "ADANIGREEN", "ADANIPOWER", "JSWENERGY", "GAIL", "OIL",
            "PETRONET", "IGL"
        ),
        "nifty-infra" to listOf(
            "RELIANCE", "LT", "BHARTIARTL", "NTPC", "POWERGRID", "ULTRACEMCO", "GRASIM",
            "ADANIPORTS", "ONGC", "COALINDIA", "TATAPOWER", "GAIL", "AMBUJACEM",
            "SIEMENS", "ABB", "DLF", "CONCOR", "INDIGO", "IRCTC", "BHEL", "VOLTAS",
            "APOLLOHOSP", "HAVELLS", "GMRINFRA", "JSWENERGY", "ASHOKLEY", "PETRONET",
            "TATACOMM", "MRF", "BALKRISIND"
        ),
        "nifty-healthcare" to listOf(
            "SUNPHARMA", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB", "MAXHEALTH",
            "MEDANTA", "FORTIS", "ZYDUSLIFE", "LUPIN", "TORNTPHARM", "AUROPHARMA",
            "MANKIND", "SYNGENE", "LALPATHLAB", "NH", "ALKEM", "BIOCON", "IPCALAB",
            "LAURUSLABS"
        ),
        "nifty-psu-bank" to listOf(
            "SBIN", "BANKBARODA", "PNB", "CANBK", "UNIONBANK", "INDIANB", "BANKINDIA",
            "CENTRALBK", "MAHABANK", "UCOBANK", "IOB", "PSB"
        ),
        "nifty-private-bank" to listOf(
            "HDFCBANK", "ICICIBANK", "AXISBANK", "KOTAKBANK", "INDUSINDBK", "FEDERALBNK",
            "IDFCFIRSTB", "AUBANK", "BANDHANBNK", "CITYUNIONB"
        ),
        "nifty-consumer-durables" to listOf(
            "TITAN", "HAVELLS", "DIXON", "VOLTAS", "BLUESTARCO", "CROMPTON", "WHIRLPOOL",
            "KAJARIACER", "BATAINDIA", "VGUARD", "AMBER", "RAJESHEXPO", "CENTURYPLY",
            "SYMPHONY", "ORIENTELEC"
        ),
        "nifty-midcap-50" to listOf(
            "POLYCAB", "TRENT", "PERSISTENT", "COFORGE", "FEDERALBNK", "IDFCFIRSTB",
            "MAXHEALTH", "BHARATFORG", "TVSMOTOR", "ASHOKLEY", "CUMMINSIND", "AUROPHARMA",
            "LUPIN", "PIIND", "ASTRAL", "JUBLFOOD", "DALBHARAT", "OBEROIRLTY", "ESCORTS",
            "MFSL", "HINDPETRO", "SUNDARMFIN", "GMRINFRA", "TATACOMM", "LICHSGFIN",
            "GODREJPROP", "DEEPAKNTR", "IPCALAB", "GLENMARK", "COLPAL", "MPHASIS",
            "APOLLOTYRE", "BALKRISIND", "VOLTAS", "DIXON", "BATAINDIA", "BANDHANBNK",
            "MRF", "NATIONALUM", "NMDC", "SAIL", "CANBK", "UNIONBANK", "INDIANB",
            "UBL", "RADICO", "PAGEIND", "ALKEM", "BIOCON", "CONCOR"
        ),
        "nifty-midcap-100" to listOf(
            "POLYCAB", "TRENT", "PERSISTENT", "COFORGE", "FEDERALBNK", "IDFCFIRSTB",
            "MAXHEALTH", "BHARATFORG", "TVSMOTOR", "ASHOKLEY", "CUMMINSIND", "AUROPHARMA",
            "LUPIN", "PIIND", "ASTRAL", "JUBLFOOD", "DALBHARAT", "OBEROIRLTY", "ESCORTS",
            "MFSL", "HINDPETRO", "SUNDARMFIN", "GMRINFRA", "TATACOMM", "LICHSGFIN",
            "GODREJPROP", "DEEPAKNTR", "IPCALAB", "GLENMARK", "COLPAL", "MPHASIS",
            "APOLLOTYRE", "BALKRISIND", "VOLTAS", "DIXON", "BATAINDIA", "BANDHANBNK",
            "MRF", "NATIONALUM", "NMDC", "SAIL", "CANBK", "UNIONBANK", "INDIANB",
            "UBL", "RADICO", "PAGEIND", "ALKEM", "BIOCON", "CONCOR", "SUZLON", "EXIDEIND",
            "CDSL", "ANGELONE", "BSE", "HUDCO", "NBCC", "RITES", "MAZDOCK", "COCHINSHIP"
        ),
        "nifty-smallcap-100" to listOf(
            "SUZLON", "EXIDEIND", "CDSL", "ANGELONE", "BSE", "HUDCO", "NBCC", "RITES",
            "MAZDOCK", "COCHINSHIP", "CESC", "CASTROLIND", "KEC", "KARURVYSYA", "MANAPPURAM",
            "SONACOMS", "JBCHEPHARM", "TATAINVEST", "TEJASNET", "CYIENT", "AMARAJABAT",
            "CAMS", "CENTRALBK", "TRIDENT", "HFCL", "PNBHOUSING", "WELSPUNLIV", "KAYNES",
            "POONAWALLA", "BLS"
        ),
        "nifty-100" to listOf(
            "RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS", "ITC", "LT", "BHARTIARTL",
            "SBIN", "AXISBANK", "KOTAKBANK", "BAJFINANCE", "MARUTI", "SUNPHARMA", "TITAN",
            "TATASTEEL", "TATAMOTORS", "HINDUNILVR", "NTPC", "POWERGRID", "COALINDIA",
            "ADANIENT", "ADANIPORTS", "M&M", "ULTRACEMCO", "GRASIM", "JSWSTEEL", "HINDALCO",
            "BPCL", "ONGC", "ASIANPAINT", "HCLTECH", "WIPRO", "BAJAJFINSV", "BAJAJ-AUTO",
            "NESTLEIND", "TECHM", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB", "EICHERMOT",
            "HEROMOTOCO", "BRITANNIA", "INDUSINDBK", "TATACONSUM", "SBILIFE", "HDFCLIFE",
            "LTIM", "BEL", "ZOMATO", "JIOFIN", "VEDL", "HAL", "CHOLAFIN", "TORNTPHARM",
            "DLF", "SIEMENS", "PIDILITIND", "BANKBARODA", "PNB", "CANBK", "TRENT",
            "AMBUJACEM", "SHRIRAMFIN", "MOTHERSON", "GAIL", "GODREJCP", "DABUR", "MARICO",
            "BERGEPAINT", "ICICIPRULI", "ICICIGI", "SBICARD", "HDFCAMC", "ABB", "BOSCHLTD",
            "HAVELLS", "INDIGO", "IRCTC", "PFC", "RECLTD", "RVNL", "IRFC", "NHPC", "IOC",
            "BHEL", "POLICYBZR", "NAUKRI", "TATACOMM", "TATAPOWER", "JSWENERGY", "ADANIPOWER",
            "ADANIGREEN", "ATGL", "CGPOWER", "PERSISTENT", "MUTHOOTFIN", "COLPAL", "POLYCAB"
        ),
        "nifty-200" to listOf(
            "RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS", "ITC", "LT", "BHARTIARTL",
            "SBIN", "AXISBANK", "KOTAKBANK", "BAJFINANCE", "MARUTI", "SUNPHARMA", "TITAN",
            "TATASTEEL", "TATAMOTORS", "HINDUNILVR", "NTPC", "POWERGRID", "COALINDIA",
            "ADANIENT", "ADANIPORTS", "M&M", "ULTRACEMCO", "GRASIM", "JSWSTEEL", "HINDALCO",
            "BPCL", "ONGC", "ASIANPAINT", "HCLTECH", "WIPRO", "BAJAJFINSV", "BAJAJ-AUTO",
            "NESTLEIND", "TECHM", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB", "EICHERMOT",
            "HEROMOTOCO", "BRITANNIA", "INDUSINDBK", "TATACONSUM", "SBILIFE", "HDFCLIFE",
            "LTIM", "BEL", "ZOMATO", "JIOFIN", "VEDL", "HAL", "CHOLAFIN", "TORNTPHARM",
            "DLF", "SIEMENS", "PIDILITIND", "BANKBARODA", "PNB", "CANBK", "TRENT",
            "AMBUJACEM", "SHRIRAMFIN", "MOTHERSON", "GAIL", "GODREJCP", "DABUR", "MARICO",
            "POLYCAB", "PERSISTENT", "COFORGE", "FEDERALBNK", "IDFCFIRSTB", "MAXHEALTH",
            "BHARATFORG", "TVSMOTOR", "ASHOKLEY", "VOLTAS", "DIXON", "SUZLON", "CDSL",
            "ANGELONE", "BSE", "HUDCO", "NBCC", "COCHINSHIP", "MAZDOCK"
        ),
        "nifty-500" to listOf(
            "RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS", "ITC", "LT", "BHARTIARTL",
            "SBIN", "AXISBANK", "KOTAKBANK", "BAJFINANCE", "MARUTI", "SUNPHARMA", "TITAN",
            "TATASTEEL", "TATAMOTORS", "HINDUNILVR", "NTPC", "POWERGRID", "COALINDIA",
            "ADANIENT", "ADANIPORTS", "M&M", "ULTRACEMCO", "GRASIM", "JSWSTEEL", "HINDALCO",
            "BPCL", "ONGC", "ASIANPAINT", "HCLTECH", "WIPRO", "BAJAJFINSV", "BAJAJ-AUTO",
            "NESTLEIND", "TECHM", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB", "EICHERMOT",
            "HEROMOTOCO", "BRITANNIA", "INDUSINDBK", "TATACONSUM", "SBILIFE", "HDFCLIFE",
            "LTIM", "BEL", "ZOMATO", "JIOFIN", "VEDL", "HAL", "CHOLAFIN", "TORNTPHARM",
            "DLF", "SIEMENS", "PIDILITIND", "BANKBARODA", "PNB", "CANBK", "TRENT",
            "AMBUJACEM", "SHRIRAMFIN", "MOTHERSON", "GAIL", "GODREJCP", "DABUR", "MARICO",
            "POLYCAB", "PERSISTENT", "COFORGE", "FEDERALBNK", "IDFCFIRSTB", "MAXHEALTH",
            "BHARATFORG", "TVSMOTOR", "ASHOKLEY", "VOLTAS", "DIXON", "SUZLON", "CDSL",
            "ANGELONE", "BSE", "HUDCO", "NBCC", "COCHINSHIP", "MAZDOCK"
        )
    )

    fun getConstituentsForIndex(indexSymbolOrId: String): List<StockSymbol> {
        val normalizedKey = indexSymbolOrId.lowercase()
            .trim()
            .replace(" ", "-")
            .replace("_", "-")

        val symbols = CONSTITUENTS_MAP[normalizedKey]
            ?: CONSTITUENTS_MAP.entries.find { it.key.contains(normalizedKey) || normalizedKey.contains(it.key) }?.value
            ?: CONSTITUENTS_MAP["nifty-50"]!!

        return symbols.map { sym ->
            STOCK_MASTER[sym] ?: createStock(sym, "$sym Ltd", "1000")
        }
    }
}
