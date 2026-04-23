import express, { Express, Request, Response } from 'express';
import dotenv from 'dotenv';

// Konfiguracja zmiennych środowiskowych
dotenv.config();

const app: Express = express();
const port = process.env.PORT || 3000;

// Middleware do parsowania JSON
app.use(express.json());

// Podstawowy routing (później przeniesiemy to do osobnych plików)
app.get('/health', (req: Request, res: Response) => {
  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    app: 'WorkDone API'
  });
});

app.listen(port, () => {
  console.log(`[server]: Server is running at http://localhost:${port}`);
});