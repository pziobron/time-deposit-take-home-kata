# API contract

The contract is defined in [openapi.yaml](src/main/resources/openapi.yaml).
It specifies exactly two operations:

- `GET /time-deposits`: retrieve all deposits, including their withdrawals.
- `POST /time-deposits/update-balances`: apply one interest update to all deposits;
  return `204 No Content` after saving the updates.

## Assumptions

Each POST triggers one update rather than checking whether a calendar month has
elapsed. Repeating it can compound interest. All deposits are updated in one
transaction; their days and withdrawal records are unchanged. Unknown plan names
continue to receive no interest. Existing calculator arithmetic is preserved.

Nested withdrawals expose the fields specified for the withdrawals table,
including `timeDepositId`. They are returned as recorded history; the requirements
do not specify deducting them during interest calculation. Collection order is
unspecified. Empty collections are represented as arrays, not nulls.

Money is represented as JSON numbers without a two-decimal-place restriction.
The database will use decimal columns, while the shared calculator retains its
existing `Double` contract. Mapping at that boundary must preserve the calculator's
existing behavior rather than introduce a new rounding rule.

## Trying the contract

The server is not implemented yet. The commands below describe the intended
interface once the application is running locally on port 8080:

```sh
curl http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/update-balances
```

To inspect the contract now, open the YAML in a Swagger-compatible editor, such as
[Swagger Editor](https://editor.swagger.io/), using **File > Import file**. Once the
server is running, use **Try it out** and **Execute** for either operation. A hosted
editor requires the local server to permit its browser origin; local Swagger UI
setup and application startup instructions will be documented with implementation.
