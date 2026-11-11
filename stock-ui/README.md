# Market Ledger – React + Vite + Tailwind frontend

    npm install
    npm run dev        # http://localhost:5173  (proxies /api -> http://localhost:8080)
    npm run build      # production build, or: docker build -t stock-ui .

Docker-compose snippet (service name `backend` must match nginx.conf):

    frontend:
      build: ./stock-ui
      ports: ["3000:80"]
      depends_on: [backend]

## Pages / RBAC
| Route | Who | Backend |
|---|---|---|
| /login | all | POST /api/v1/auth/login |
| / (dashboard) | Admin, Viewer | GET /api/v1/stocks/analytics |
| /stocks | Admin, Viewer | GET /api/v1/stocks |
| /upload | Admin | POST /api/v1/admin/files/upload, GET /api/v1/admin/files/{id}/status |
| /users | Admin | /api/v1/admin/users/** |

Nav links are hidden and routes are guarded for non-admins (the API stays the real gate).

## Backend changes needed
1. `StockDataController.getStockData`: change to `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` (viewers must read data).
2. Add `GET /api/v1/stocks/analytics` (any role) – params `ticker,startDate,endDate`, returns
   `{ totalVolume, averageClose, tradingDays, anomalyCount, priceSeries:[{date,avgClose,movingAvg}], volumeByTicker:[{ticker,volume}] }`.
3. Add `GET /api/v1/admin/files/{id}/status` returning `{ id, status, processedRows, totalRows, failedRows, message }`
   with status in `PENDING | PROCESSING | COMPLETED | FAILED`.
4. Confirm the login path (`/api/v1/auth/login`) matches your `AuthController` `@RequestMapping`; all paths live in `src/api.js`.
