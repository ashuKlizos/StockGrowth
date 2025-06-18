import React from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Typography,
    Box,
} from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import { StockAnalysis } from '../types/stock';

interface StockTableProps {
    stocks: StockAnalysis[];
    title: string;
}

export const StockTable: React.FC<StockTableProps> = ({ stocks, title }) => {
    const formatNumber = (num: number) => {
        return new Intl.NumberFormat('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        }).format(num);
    };

    const formatMarketCap = (marketCap: number) => {
        if (marketCap >= 1e9) return `$${(marketCap / 1e9).toFixed(2)}B`;
        if (marketCap >= 1e6) return `$${(marketCap / 1e6).toFixed(2)}M`;
        return `$${formatNumber(marketCap)}`;
    };

    const formatVolume = (volume: number) => {
        if (volume >= 1e6) return `${(volume / 1e6).toFixed(2)}M`;
        if (volume >= 1e3) return `${(volume / 1e3).toFixed(2)}K`;
        return volume.toString();
    };

    return (
        <Box sx={{ width: '100%', mb: 4 }}>
            <Typography variant="h5" sx={{ mb: 2 }}>
                {title}
            </Typography>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Ticker</TableCell>
                            <TableCell>Company</TableCell>
                            <TableCell align="right">Market Cap</TableCell>
                            <TableCell align="right">Volume</TableCell>
                            <TableCell align="right">1d Change</TableCell>
                            <TableCell align="right">5d Change</TableCell>
                            <TableCell align="right">30d Change</TableCell>
                            <TableCell align="center">Trend</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {stocks.map((stock) => (
                            <TableRow key={stock.ticker}>
                                <TableCell component="th" scope="row">
                                    <strong>{stock.ticker}</strong>
                                </TableCell>
                                <TableCell>{stock.companyName}</TableCell>
                                <TableCell align="right">
                                    {formatMarketCap(stock.marketCap)}
                                </TableCell>
                                <TableCell align="right">
                                    {formatVolume(stock.volume)}
                                    {stock.hasUnusualVolume && (
                                        <Typography
                                            component="span"
                                            sx={{ color: 'warning.main', ml: 1 }}
                                        >
                                            ⚡
                                        </Typography>
                                    )}
                                </TableCell>
                                <TableCell
                                    align="right"
                                    sx={{
                                        color: stock.priceChange1d >= 0 ? 'success.main' : 'error.main',
                                    }}
                                >
                                    {formatNumber(stock.priceChange1d)}%
                                </TableCell>
                                <TableCell
                                    align="right"
                                    sx={{
                                        color: stock.priceChange5d >= 0 ? 'success.main' : 'error.main',
                                    }}
                                >
                                    {formatNumber(stock.priceChange5d)}%
                                </TableCell>
                                <TableCell
                                    align="right"
                                    sx={{
                                        color: stock.priceChange30d >= 0 ? 'success.main' : 'error.main',
                                    }}
                                >
                                    {formatNumber(stock.priceChange30d)}%
                                </TableCell>
                                <TableCell align="center">
                                    {stock.isUptrending ? (
                                        <TrendingUpIcon color="success" />
                                    ) : (
                                        <TrendingDownIcon color="error" />
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}; 