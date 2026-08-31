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
            token = "99926004",
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
            token = "99926002",
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
            token = "99926006",
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
            token = "99926017",
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
            token = "99926012",
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
            token = "99926019",
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
            token = "99926011",
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
            token = "99926005",
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
            token = "99926007",
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
            token = "99926018",
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
            token = "99926022",
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
            token = "99926013",
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
            token = "99926020",
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
            token = "99926001",
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
            token = "99926015",
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
            token = "99926023",
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

    // Comprehensive stock details master map
    private val STOCK_MASTER: Map<String, StockSymbol> = mapOf(
        "RELIANCE" to StockSymbol("RELIANCE", "Reliance Industries Ltd", "2885", "NSE", 2980.40, 32.60, 1.11, 2955.00, 2992.00, 2948.00, 2980.40, 5420000, 2947.80),
        "HDFCBANK" to StockSymbol("HDFCBANK", "HDFC Bank Ltd", "1333", "NSE", 1642.15, -8.45, -0.51, 1655.00, 1660.00, 1638.50, 1642.15, 8910000, 1650.60),
        "ICICIBANK" to StockSymbol("ICICIBANK", "ICICI Bank Ltd", "4963", "NSE", 1198.80, 14.20, 1.20, 1188.00, 1204.00, 1185.00, 1198.80, 7840000, 1184.60),
        "INFY" to StockSymbol("INFY", "Infosys Ltd", "1594", "NSE", 1795.50, 21.30, 1.20, 1778.00, 1805.00, 1772.00, 1795.50, 6120000, 1774.20),
        "TCS" to StockSymbol("TCS", "Tata Consultancy Services", "11536", "NSE", 4185.00, 45.20, 1.09, 4145.00, 4205.00, 4138.00, 4185.00, 2340000, 4139.80),
        "ITC" to StockSymbol("ITC", "ITC Ltd", "1660", "NSE", 468.90, -1.20, -0.26, 471.00, 473.00, 467.50, 468.90, 4510000, 470.10),
        "LT" to StockSymbol("LT", "Larsen & Toubro Ltd", "11483", "NSE", 3620.00, -25.00, -0.69, 3650.00, 3662.00, 3608.00, 3620.00, 1820000, 3645.00),
        "BHARTIARTL" to StockSymbol("BHARTIARTL", "Bharti Airtel Ltd", "10604", "NSE", 1485.00, 18.50, 1.26, 1470.00, 1492.00, 1466.00, 1485.00, 3900000, 1466.50),
        "SBIN" to StockSymbol("SBIN", "State Bank of India", "3045", "NSE", 812.30, 3.80, 0.47, 810.00, 818.50, 807.00, 812.30, 11200000, 808.50),
        "AXISBANK" to StockSymbol("AXISBANK", "Axis Bank Ltd", "5900", "NSE", 1175.20, 6.30, 0.54, 1170.00, 1182.00, 1166.00, 1175.20, 4520000, 1168.90),
        "KOTAKBANK" to StockSymbol("KOTAKBANK", "Kotak Mahindra Bank Ltd", "1922", "NSE", 1785.40, 13.30, 0.75, 1775.00, 1794.00, 1768.00, 1785.40, 3210000, 1772.10),
        "BAJFINANCE" to StockSymbol("BAJFINANCE", "Bajaj Finance Ltd", "317", "NSE", 6920.00, 40.00, 0.58, 6890.00, 6950.00, 6860.00, 6920.00, 1420000, 6880.00),
        "MARUTI" to StockSymbol("MARUTI", "Maruti Suzuki India Ltd", "10999", "NSE", 12450.00, 70.00, 0.57, 12400.00, 12510.00, 12360.00, 12450.00, 740000, 12380.00),
        "SUNPHARMA" to StockSymbol("SUNPHARMA", "Sun Pharmaceutical Ltd", "3351", "NSE", 1680.00, 8.00, 0.48, 1675.00, 1690.00, 1668.00, 1680.00, 2180000, 1672.00),
        "TITAN" to StockSymbol("TITAN", "Titan Company Ltd", "3506", "NSE", 3580.00, 20.00, 0.56, 3565.00, 3595.00, 3550.00, 3580.00, 1120000, 3560.00),
        "TATASTEEL" to StockSymbol("TATASTEEL", "Tata Steel Ltd", "3499", "NSE", 154.20, 1.10, 0.72, 153.50, 155.40, 152.80, 154.20, 24500000, 153.10),
        "TATAMOTORS" to StockSymbol("TATAMOTORS", "Tata Motors Ltd", "3456", "NSE", 984.60, -12.40, -1.24, 1002.00, 1005.00, 980.20, 984.60, 9530000, 997.00),
        "HINDUNILVR" to StockSymbol("HINDUNILVR", "Hindustan Unilever Ltd", "1394", "NSE", 2740.00, 15.00, 0.55, 2730.00, 2755.00, 2720.00, 2740.00, 1680000, 2725.00),
        "NTPC" to StockSymbol("NTPC", "NTPC Ltd", "11630", "NSE", 412.50, 2.70, 0.66, 410.00, 415.00, 408.00, 412.50, 13400000, 409.80),
        "POWERGRID" to StockSymbol("POWERGRID", "Power Grid Corp of India", "14977", "NSE", 328.00, 2.60, 0.80, 326.00, 330.50, 324.00, 328.00, 11200000, 325.40),
        "COALINDIA" to StockSymbol("COALINDIA", "Coal India Ltd", "20374", "NSE", 512.00, 3.80, 0.75, 509.00, 516.00, 507.00, 512.00, 8900000, 508.20),
        "ADANIENT" to StockSymbol("ADANIENT", "Adani Enterprises Ltd", "25", "NSE", 3040.00, 25.00, 0.83, 3020.00, 3065.00, 3010.00, 3040.00, 2140000, 3015.00),
        "ADANIPORTS" to StockSymbol("ADANIPORTS", "Adani Ports & SEZ Ltd", "15083", "NSE", 1460.00, 15.00, 1.04, 1450.00, 1472.00, 1442.00, 1460.00, 3600000, 1445.00),
        "M&M" to StockSymbol("M&M", "Mahindra & Mahindra Ltd", "2031", "NSE", 2780.00, 30.00, 1.09, 2760.00, 2795.00, 2745.00, 2780.00, 2950000, 2750.00),
        "ULTRACEMCO" to StockSymbol("ULTRACEMCO", "UltraTech Cement Ltd", "11532", "NSE", 11420.00, 70.00, 0.62, 11380.00, 11480.00, 11320.00, 11420.00, 380000, 11350.00),
        "GRASIM" to StockSymbol("GRASIM", "Grasim Industries Ltd", "1232", "NSE", 2680.00, 20.00, 0.75, 2665.00, 2695.00, 2650.00, 2680.00, 840000, 2660.00),
        "JSWSTEEL" to StockSymbol("JSWSTEEL", "JSW Steel Ltd", "11723", "NSE", 945.00, 7.00, 0.75, 940.00, 951.00, 936.00, 945.00, 4200000, 938.00),
        "HINDALCO" to StockSymbol("HINDALCO", "Hindalco Industries Ltd", "1363", "NSE", 685.00, 6.00, 0.88, 680.00, 689.00, 677.00, 685.00, 6800000, 679.00),
        "BPCL" to StockSymbol("BPCL", "Bharat Petroleum Corp Ltd", "526", "NSE", 345.00, 3.00, 0.88, 343.00, 348.00, 341.00, 345.00, 9400000, 342.00),
        "ONGC" to StockSymbol("ONGC", "Oil & Natural Gas Corp Ltd", "2475", "NSE", 318.00, 3.00, 0.95, 316.00, 321.00, 314.00, 318.00, 14200000, 315.00),
        "ASIANPAINT" to StockSymbol("ASIANPAINT", "Asian Paints Ltd", "236", "NSE", 3120.00, 30.00, 0.97, 3100.00, 3135.00, 3085.00, 3120.00, 1150000, 3090.00),
        "HCLTECH" to StockSymbol("HCLTECH", "HCL Technologies Ltd", "7229", "NSE", 1780.00, 20.00, 1.14, 1765.00, 1792.00, 1758.00, 1780.00, 3200000, 1760.00),
        "WIPRO" to StockSymbol("WIPRO", "Wipro Ltd", "3787", "NSE", 524.00, 6.00, 1.16, 520.00, 527.00, 517.00, 524.00, 6700000, 518.00),
        "BAJAJFINSV" to StockSymbol("BAJAJFINSV", "Bajaj Finserv Ltd", "16675", "NSE", 1780.00, 15.00, 0.85, 1770.00, 1790.00, 1762.00, 1780.00, 1920000, 1765.00),
        "BAJAJ-AUTO" to StockSymbol("BAJAJ-AUTO", "Bajaj Auto Ltd", "16669", "NSE", 10450.00, 70.00, 0.67, 10400.00, 10520.00, 10360.00, 10450.00, 480000, 10380.00),
        "NESTLEIND" to StockSymbol("NESTLEIND", "Nestle India Ltd", "17963", "NSE", 2480.00, 20.00, 0.81, 2465.00, 2495.00, 2455.00, 2480.00, 620000, 2460.00),
        "TECHM" to StockSymbol("TECHM", "Tech Mahindra Ltd", "13538", "NSE", 1560.00, 15.00, 0.97, 1550.00, 1572.00, 1542.00, 1560.00, 2450000, 1545.00),
        "DRREDDY" to StockSymbol("DRREDDY", "Dr Reddys Laboratories Ltd", "881", "NSE", 6720.00, 40.00, 0.60, 6690.00, 6750.00, 6660.00, 6720.00, 680000, 6680.00),
        "CIPLA" to StockSymbol("CIPLA", "Cipla Ltd", "694", "NSE", 1580.00, 15.00, 0.96, 1570.00, 1590.00, 1562.00, 1580.00, 2200000, 1565.00),
        "APOLLOHOSP" to StockSymbol("APOLLOHOSP", "Apollo Hospitals Enterprise", "157", "NSE", 6940.00, 50.00, 0.73, 6900.00, 6975.00, 6880.00, 6940.00, 710000, 6890.00),
        "DIVISLAB" to StockSymbol("DIVISLAB", "Divis Laboratories Ltd", "10940", "NSE", 4950.00, 40.00, 0.81, 4920.00, 4980.00, 4890.00, 4950.00, 780000, 4910.00),
        "EICHERMOT" to StockSymbol("EICHERMOT", "Eicher Motors Ltd", "910", "NSE", 4890.00, 40.00, 0.82, 4860.00, 4920.00, 4840.00, 4890.00, 890000, 4850.00),
        "HEROMOTOCO" to StockSymbol("HEROMOTOCO", "Hero MotoCorp Ltd", "1348", "NSE", 5420.00, 40.00, 0.74, 5390.00, 5450.00, 5370.00, 5420.00, 620000, 5380.00),
        "BRITANNIA" to StockSymbol("BRITANNIA", "Britannia Industries Ltd", "547", "NSE", 5890.00, 50.00, 0.86, 5850.00, 5920.00, 5830.00, 5890.00, 520000, 5840.00),
        "INDUSINDBK" to StockSymbol("INDUSINDBK", "IndusInd Bank Ltd", "5258", "NSE", 1420.00, 12.00, 0.85, 1410.00, 1430.00, 1402.00, 1420.00, 3100000, 1408.00),
        "TATACONSUM" to StockSymbol("TATACONSUM", "Tata Consumer Products Ltd", "3432", "NSE", 1180.00, 12.00, 1.03, 1170.00, 1188.00, 1165.00, 1180.00, 2400000, 1168.00),
        "SBILIFE" to StockSymbol("SBILIFE", "SBI Life Insurance Co Ltd", "21808", "NSE", 1780.00, 20.00, 1.14, 1765.00, 1792.00, 1758.00, 1780.00, 1450000, 1760.00),
        "HDFCLIFE" to StockSymbol("HDFCLIFE", "HDFC Life Insurance Co Ltd", "467", "NSE", 720.00, 8.00, 1.12, 714.00, 725.00, 710.00, 720.00, 4800000, 712.00),
        "LTIM" to StockSymbol("LTIM", "LTIMindtree Ltd", "17818", "NSE", 5980.00, 60.00, 1.01, 5940.00, 6010.00, 5910.00, 5980.00, 640000, 5920.00),
        "BEL" to StockSymbol("BEL", "Bharat Electronics Ltd", "383", "NSE", 305.00, 3.50, 1.16, 302.00, 308.00, 300.50, 305.00, 18500000, 301.50),
        "ZOMATO" to StockSymbol("ZOMATO", "Zomato Ltd", "5097", "NSE", 258.00, 6.00, 2.38, 253.00, 261.00, 251.50, 258.00, 32000000, 252.00),
        "JIOFIN" to StockSymbol("JIOFIN", "Jio Financial Services Ltd", "18143", "NSE", 325.00, 5.00, 1.56, 321.00, 328.00, 319.00, 325.00, 28000000, 320.00),
        "VEDL" to StockSymbol("VEDL", "Vedanta Ltd", "3063", "NSE", 462.00, 6.00, 1.32, 458.00, 466.00, 455.00, 462.00, 16000000, 456.00),
        "HAL" to StockSymbol("HAL", "Hindustan Aeronautics Ltd", "2303", "NSE", 4720.00, 40.00, 0.85, 4690.00, 4750.00, 4670.00, 4720.00, 1950000, 4680.00),
        "DLF" to StockSymbol("DLF", "DLF Ltd", "14732", "NSE", 845.00, 7.00, 0.84, 840.00, 852.00, 836.00, 845.00, 6200000, 838.00),
        "PIDILITIND" to StockSymbol("PIDILITIND", "Pidilite Industries Ltd", "2664", "NSE", 3180.00, 30.00, 0.95, 3160.00, 3200.00, 3140.00, 3180.00, 820000, 3150.00),
        "BANKBARODA" to StockSymbol("BANKBARODA", "Bank of Baroda", "4668", "NSE", 254.00, 3.00, 1.20, 252.00, 256.50, 250.00, 254.00, 14200000, 251.00),
        "PNB" to StockSymbol("PNB", "Punjab National Bank", "10666", "NSE", 112.50, 1.70, 1.53, 111.00, 114.00, 110.20, 112.50, 28000000, 110.80),
        "CANBK" to StockSymbol("CANBK", "Canara Bank", "10794", "NSE", 108.00, 1.50, 1.41, 107.00, 109.50, 106.00, 108.00, 21000000, 106.50),
        "TRENT" to StockSymbol("TRENT", "Trent Ltd", "1964", "NSE", 7120.00, 80.00, 1.14, 7060.00, 7180.00, 7020.00, 7120.00, 1100000, 7040.00),
        "AMBUJACEM" to StockSymbol("AMBUJACEM", "Ambuja Cements Ltd", "1270", "NSE", 635.00, 7.00, 1.11, 630.00, 640.00, 626.00, 635.00, 4800000, 628.00),
        "SHRIRAMFIN" to StockSymbol("SHRIRAMFIN", "Shriram Finance Ltd", "4306", "NSE", 3250.00, 40.00, 1.25, 3220.00, 3280.00, 3200.00, 3250.00, 1350000, 3210.00),
        "GAIL" to StockSymbol("GAIL", "GAIL (India) Ltd", "4717", "NSE", 228.00, 3.50, 1.56, 225.00, 230.00, 224.00, 228.00, 16500000, 224.50),
        "GODREJCP" to StockSymbol("GODREJCP", "Godrej Consumer Products", "10099", "NSE", 1420.00, 15.00, 1.07, 1410.00, 1432.00, 1400.00, 1420.00, 1650000, 1405.00),
        "DABUR" to StockSymbol("DABUR", "Dabur India Ltd", "772", "NSE", 645.00, 5.00, 0.78, 642.00, 649.00, 638.00, 645.00, 2800000, 640.00),
        "MARICO" to StockSymbol("MARICO", "Marico Ltd", "4067", "NSE", 654.00, 6.00, 0.93, 650.00, 658.00, 646.00, 654.00, 2900000, 648.00),
        "HAVELLS" to StockSymbol("HAVELLS", "Havells India Ltd", "9819", "NSE", 1980.00, 20.00, 1.02, 1965.00, 1995.00, 1955.00, 1980.00, 1450000, 1960.00),
        "INDIGO" to StockSymbol("INDIGO", "InterGlobe Aviation Ltd", "11195", "NSE", 4680.00, 60.00, 1.30, 4640.00, 4710.00, 4610.00, 4680.00, 1300000, 4620.00),
        "IRCTC" to StockSymbol("IRCTC", "Indian Railway Catering & Tourism", "13611", "NSE", 920.00, 8.00, 0.88, 915.00, 928.00, 910.00, 920.00, 3900000, 912.00),
        "PFC" to StockSymbol("PFC", "Power Finance Corporation Ltd", "14299", "NSE", 512.00, 6.00, 1.19, 508.00, 516.00, 505.00, 512.00, 9200000, 506.00),
        "RECLTD" to StockSymbol("RECLTD", "REC Ltd", "15355", "NSE", 585.00, 7.00, 1.21, 580.00, 591.00, 576.00, 585.00, 8400000, 578.00),
        "IOC" to StockSymbol("IOC", "Indian Oil Corporation Ltd", "1624", "NSE", 172.00, 2.00, 1.18, 171.00, 174.00, 169.50, 172.00, 18500000, 170.00),
        "BHEL" to StockSymbol("BHEL", "Bharat Heavy Electricals Ltd", "438", "NSE", 285.00, 5.00, 1.79, 282.00, 289.00, 279.00, 285.00, 16200000, 280.00),
        "TATAPOWER" to StockSymbol("TATAPOWER", "Tata Power Co Ltd", "3426", "NSE", 435.00, 7.00, 1.64, 430.00, 439.00, 427.00, 435.00, 12800000, 428.00),
        "POLYCAB" to StockSymbol("POLYCAB", "Polycab India Ltd", "9590", "NSE", 6940.00, 60.00, 0.87, 6900.00, 6980.00, 6860.00, 6940.00, 680000, 6880.00),
        "PERSISTENT" to StockSymbol("PERSISTENT", "Persistent Systems Ltd", "18365", "NSE", 5120.00, 60.00, 1.19, 5080.00, 5160.00, 5050.00, 5120.00, 720000, 5060.00),
        "COFORGE" to StockSymbol("COFORGE", "Coforge Ltd", "11543", "NSE", 6780.00, 70.00, 1.04, 6730.00, 6820.00, 6690.00, 6780.00, 580000, 6710.00),
        "FEDERALBNK" to StockSymbol("FEDERALBNK", "The Federal Bank Ltd", "1023", "NSE", 198.00, 3.00, 1.54, 196.00, 200.00, 194.50, 198.00, 11500000, 195.00),
        "IDFCFIRSTB" to StockSymbol("IDFCFIRSTB", "IDFC FIRST Bank Ltd", "11184", "NSE", 76.50, 0.70, 0.92, 76.00, 77.20, 75.40, 76.50, 24000000, 75.80),
        "AUBANK" to StockSymbol("AUBANK", "AU Small Finance Bank Ltd", "21238", "NSE", 685.00, 7.00, 1.03, 680.00, 692.00, 676.00, 685.00, 3200000, 678.00),
        "BANDHANBNK" to StockSymbol("BANDHANBNK", "Bandhan Bank Ltd", "2263", "NSE", 204.00, 3.00, 1.49, 202.00, 206.50, 200.50, 204.00, 8900000, 201.00),
        "UNIONBANK" to StockSymbol("UNIONBANK", "Union Bank of India", "10606", "NSE", 128.00, 2.00, 1.59, 126.50, 129.50, 125.50, 128.00, 15500000, 126.00),
        "INDIANB" to StockSymbol("INDIANB", "Indian Bank", "10815", "NSE", 545.00, 7.00, 1.30, 540.00, 550.00, 536.00, 545.00, 2800000, 538.00),
        "VOLTAS" to StockSymbol("VOLTAS", "Voltas Ltd", "3718", "NSE", 1780.00, 25.00, 1.42, 1760.00, 1795.00, 1750.00, 1780.00, 1850000, 1755.00),
        "DIXON" to StockSymbol("DIXON", "Dixon Technologies Ltd", "21690", "NSE", 12850.00, 160.00, 1.26, 12720.00, 12950.00, 12650.00, 12850.00, 420000, 12690.00),
        "SUZLON" to StockSymbol("SUZLON", "Suzlon Energy Ltd", "13528", "NSE", 78.50, 1.70, 2.21, 77.00, 79.80, 76.50, 78.50, 48000000, 76.80),
        "CDSL" to StockSymbol("CDSL", "Central Depository Services", "21174", "NSE", 1480.00, 30.00, 2.07, 1460.00, 1495.00, 1445.00, 1480.00, 3100000, 1450.00),
        "ANGELONE" to StockSymbol("ANGELONE", "Angel One Ltd", "87", "NSE", 2680.00, 40.00, 1.52, 2650.00, 2710.00, 2630.00, 2680.00, 1450000, 2640.00),
        "BSE" to StockSymbol("BSE", "BSE Ltd", "19585", "NSE", 2940.00, 50.00, 1.73, 2900.00, 2970.00, 2880.00, 2940.00, 2800000, 2890.00),
        "HUDCO" to StockSymbol("HUDCO", "Housing & Urban Development", "20935", "NSE", 285.00, 7.00, 2.52, 280.00, 289.00, 277.00, 285.00, 9400000, 278.00),
        "NBCC" to StockSymbol("NBCC", "NBCC (India) Ltd", "18096", "NSE", 178.00, 4.00, 2.30, 175.00, 181.00, 173.50, 178.00, 18500000, 174.00),
        "COCHINSHIP" to StockSymbol("COCHINSHIP", "Cochin Shipyard Ltd", "21469", "NSE", 1940.00, 45.00, 2.37, 1910.00, 1965.00, 1890.00, 1940.00, 4200000, 1895.00),
        "MAZDOCK" to StockSymbol("MAZDOCK", "Mazagon Dock Shipbuilders", "2643", "NSE", 4350.00, 70.00, 1.64, 4300.00, 4400.00, 4260.00, 4350.00, 1650000, 4280.00)
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
            STOCK_MASTER[sym] ?: StockSymbol(
                symbol = sym,
                name = "$sym Ltd",
                token = "1000",
                exchange = "NSE",
                ltp = 1250.00,
                change = 12.50,
                changePercent = 1.01,
                open = 1240.00,
                high = 1260.00,
                low = 1235.00,
                close = 1250.00,
                volume = 2500000,
                previousClose = 1237.50
            )
        }
    }
}
