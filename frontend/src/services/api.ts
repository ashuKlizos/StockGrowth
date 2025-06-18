import axios, { AxiosError } from 'axios';
import { StockAnalysis } from '../types/stock';

const API_BASE_URL = 'http://localhost:8080/api/stocks';

const handleError = (error: AxiosError) => {
    if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        throw new Error(`Server error: ${error.response.status} - ${error.response.data}`);
    } else if (error.request) {
        // The request was made but no response was received
        throw new Error('No response from server. Please check if the backend server is running.');
    } else {
        // Something happened in setting up the request that triggered an Error
        throw new Error(`Error: ${error.message}`);
    }
};

export const stockApi = {
    getAllStocks: async () => {
        try {
            return await axios.get<StockAnalysis[]>(`${API_BASE_URL}/analyze`);
        } catch (error) {
            handleError(error as AxiosError);
        }
    },
    
    getUptrendingStocks: async () => {
        try {
            return await axios.get<StockAnalysis[]>(`${API_BASE_URL}/analyze/uptrend`);
        } catch (error) {
            handleError(error as AxiosError);
        }
    },
    
    getVolumeSpikes: async () => {
        try {
            return await axios.get<StockAnalysis[]>(`${API_BASE_URL}/analyze/volume-spike`);
        } catch (error) {
            handleError(error as AxiosError);
        }
    },
    
    getTopGainers: async (period: string = '30') => {
        try {
            return await axios.get<StockAnalysis[]>(`${API_BASE_URL}/analyze/gainers?period=${period}`);
        } catch (error) {
            handleError(error as AxiosError);
        }
    },
    
    filterStocks: async (params: {
        minMarketCap?: number;
        maxMarketCap?: number;
        minPrice30dChange?: number;
        uptrendOnly?: boolean;
        unusualVolumeOnly?: boolean;
    }) => {
        try {
            return await axios.get<StockAnalysis[]>(`${API_BASE_URL}/analyze/filter`, { params });
        } catch (error) {
            handleError(error as AxiosError);
        }
    }
}; 