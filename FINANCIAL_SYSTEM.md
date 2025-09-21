# Financial Management System - MVT Events

## Overview

The implemented financial management system provides complete control over payments, platform fees, and automatic transfers for sports event organizers.

## Financial Architecture

### Multi-Tenant Strategy

- **Event-as-Tenant**: Each event functions as an isolated tenant
- **Row Level Security (RLS)**: Data isolation at the row level in PostgreSQL
- **Event Context**: Security based on the current event context

### Main Entities

#### 1. EventFinancials

**Table**: `event_financials`

Consolidates financial information per event:

- **Total Revenue**: Sum of all received payments
- **Platform Fees**: Percentage charged by the platform
- **Net Revenue**: Amount destined for the organizer
- **Pending Amount**: Amount awaiting transfer
- **Transferred Amount**: Total already transferred to the organizer
- **Transfer Frequency**: Immediate, daily, weekly, monthly, or on-demand

#### 2. Transfer

**Table**: `transfers`

Manages value transfers to organizers:

- **Supported Methods**: PIX, TED, Bank Transfer
- **Tracking Status**: Pending, Processing, Completed, Failed
- **Gateway Integration**: Support for multiple payment providers
- **Retry Logic**: Automatic attempts for failed transfers

#### 3. PaymentEvent

**Table**: `payment_events`

Complete audit of financial events:

- **Event Types**: Payment received, refund, fee calculated, transfer initiated
- **Traceability**: Complete log of all financial operations
- **Metadata**: Additional information in JSON format

#### 4. Payment

**Table**: `payments`

Individual payment management:

- **Complete Status**: Pending, processing, completed, failed, refunded
- **Payment Methods**: Card, PIX, bank transfer
- **Gateway Integration**: Support for multiple providers

## Main Features

### 1. Automatic Payment Processing

```java
// Usage example
FinancialService.processPayment(payment);
```

- Automatically calculates platform fee
- Updates organizer's net value
- Records audit events
- Schedules next transfer

### 2. Scheduled Transfers

```java
// Supported frequencies
TransferFrequency.IMMEDIATE  // Immediate
TransferFrequency.DAILY      // Daily
TransferFrequency.WEEKLY     // Weekly
TransferFrequency.MONTHLY    // Monthly
TransferFrequency.ON_DEMAND  // On demand
```

### 3. Automatic Retry System

- **Failed Transfers**: Automatic attempts every 4 hours
- **Attempt Limit**: Maximum 3 attempts per transfer
- **Detailed Logging**: Failure reason and attempt history

### 4. Mock Payment Gateway

Simulated implementation with:

- **PIX Validation**: PIX key format verification
- **Fee Calculation**: Based on transfer method
- **Processing Time**: Realistic estimates per method

## API Endpoints

### Financial - `/api/financial`

#### Event Financial Summary

```http
GET /api/financial/events/{eventId}/summary
Authorization: Bearer {token}
```

#### Create Manual Transfer

```http
POST /api/financial/events/{eventId}/transfers
Content-Type: application/json
Authorization: Bearer {token}

{
    "amount": 100.00,
    "transferMethod": "PIX",
    "destinationKey": "email@example.com"
}
```

#### Calculate Transfer Fee

```http
GET /api/financial/transfer-fee?amount=100.00&method=PIX
Authorization: Bearer {token}
```

#### Process Transfers (Admin)

```http
POST /api/financial/transfers/process
Authorization: Bearer {token}
```

## Configuration

### Default Fees

- **Platform Fee**: 5% (configurable per event)
- **Minimum Transfer Amount**: R$ 10.00
- **PIX Fee**: 1% of the amount
- **TED Fee**: R$ 5.00 fixed
- **Bank Transfer Fee**: R$ 2.50 fixed

### Automatic Scheduling

- **Automatic Transfers**: Every hour
- **Pending Transfers**: Every 30 minutes
- **Failure Retry**: Every 4 hours

## Security

### Row Level Security (RLS)

```sql
-- Event isolation policy
CREATE POLICY event_financials_isolation ON event_financials
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::uuid);
```

### JWT Authorization

- **ORGANIZER**: Access to own event data
- **ADMIN**: Full access to all data
- **Context Aware**: Security based on event context

## Monitoring

### Detailed Logs

- All financial operations are logged
- Automatic transfers include performance metrics
- Failures include detailed reason and stack trace

### Available Metrics

- Total pending transfers
- Total transferred volume
- Transfer success rate
- Average processing time

## Complete Flow Example

1. **Payment Received**: System processes automatically
2. **Fee Calculated**: 5% retained by platform
3. **Net Value**: Added to organizer's pending balance
4. **Scheduling**: Next transfer scheduled according to frequency
5. **Automatic Transfer**: System executes at scheduled time
6. **Confirmation**: Gateway confirms success/failure
7. **Audit**: All events recorded for compliance

## Extensibility

### New Payment Gateways

Implement `PaymentGatewayService` interface to add new providers.

### New Features

- Split payment system for multiple beneficiaries
- Escrow for events with refund policy
- Integration with accounting systems
- Real-time financial dashboard

This system provides a solid and scalable foundation for financial management of sports event platforms, with a focus on automation, security, and auditability.
