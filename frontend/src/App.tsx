import React, { useEffect, useState, useCallback } from 'react';
import {
    AppBar,
    Box,
    Container,
    Tab,
    Tabs,
    ThemeProvider,
    Toolbar,
    Typography,
    createTheme,
} from '@mui/material';
import { StockTable } from './components/StockTable';
import { stockApi } from './services/api';
import { StockAnalysis } from './types/stock';

const theme = createTheme({
    palette: {
        mode: 'dark',
    },
});

function App() {
    const [tab, setTab] = useState(0);
    const [stocks, setStocks] = useState<StockAnalysis[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchStocks = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            let response;

            switch (tab) {
                case 0: // All Stocks
                    response = await stockApi.getAllStocks();
                    break;
                case 1: // Uptrending
                    response = await stockApi.getUptrendingStocks();
                    break;
                case 2: // Volume Spikes
                    response = await stockApi.getVolumeSpikes();
                    break;
                case 3: // Top Gainers
                    response = await stockApi.getTopGainers();
                    break;
                default:
                    response = await stockApi.getAllStocks();
            }

            if (response) {
                setStocks(response.data);
            }
        } catch (err) {
            const error = err as Error;
            setError(error.message || 'Failed to fetch stock data. Please try again later.');
            console.error('Error fetching stocks:', error);
        } finally {
            setLoading(false);
        }
    }, [tab]);

    useEffect(() => {
        fetchStocks();
    }, [fetchStocks]);

    const getTitle = () => {
        switch (tab) {
            case 0:
                return 'All Stocks';
            case 1:
                return 'Uptrending Stocks';
            case 2:
                return 'Volume Spikes';
            case 3:
                return 'Top Gainers (30d)';
            default:
                return 'Stocks';
        }
    };

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setTab(newValue);
    };

    return (
        <ThemeProvider theme={theme}>
            <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: 'background.default' }}>
                <AppBar position="static">
                    <Toolbar>
                        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                            Stock Growth Analysis
                        </Typography>
                    </Toolbar>
                    <Tabs
                        value={tab}
                        onChange={handleTabChange}
                        centered
                        sx={{ bgcolor: 'background.paper' }}
                    >
                        <Tab label="All Stocks" />
                        <Tab label="Uptrending" />
                        <Tab label="Volume Spikes" />
                        <Tab label="Top Gainers" />
                    </Tabs>
                </AppBar>

                <Container maxWidth="xl" sx={{ mt: 4 }}>
                    {error ? (
                        <Typography color="error" sx={{ mt: 2 }}>
                            {error}
                        </Typography>
                    ) : loading ? (
                        <Typography sx={{ mt: 2 }}>Loading...</Typography>
                    ) : (
                        <StockTable stocks={stocks} title={getTitle()} />
                    )}
                </Container>
            </Box>
        </ThemeProvider>
    );
}

export default App;
