export interface StockAnalysis {
    ticker: string;
    companyName: string;
    marketCap: number;
    volume: number;
    priceChange1d: number;
    priceChange5d: number;
    priceChange30d: number;
    isUptrending: boolean;
    hasUnusualVolume: boolean;
    averageVolume: number;
} 