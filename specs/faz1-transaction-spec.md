# FAZ 1 — Transaction Engine Spec

## Overview
Basic account and transaction management for payment processing.

## Entities
- Account: id (UUID), ownerName, currency, balance, status, version
- Transaction: id (UUID), sourceAccountId, targetAccountId, amount, currency, status, type, createdAt

## Transaction States
PENDING -> COMPLETED | FAILED
FAILED -> REVERSED
COMPLETED -> (terminal state)

## API Endpoints
- POST /api/v1/accounts - Create account
- GET /api/v1/accounts/{id} - Get account details
- GET /api/v1/accounts/{id}/balance - Get balance
- POST /api/v1/transactions - Create transfer
- GET /api/v1/transactions/{id} - Get transaction details

## Business Rules
- Balance cannot go negative
- Cannot transfer to same account
- Minimum transfer amount: 0.01
- Maximum transfer amount: 1,000,000
- Account must be ACTIVE to send/receive
- Currency must match between accounts

## Acceptance Criteria
- [ ] Account CRUD works
- [ ] Transfer deducts from source, adds to target
- [ ] Insufficient balance returns 400
- [ ] Same account transfer returns 400
- [ ] Inactive account transfer returns 400
- [ ] All endpoints return proper error responses
