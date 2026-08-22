Iteration 498/500

→ Create Document
  POST http://localhost:8181/documents [200 OK, 296B, 33ms]
  √  Status code is 200
  √  Response has accountId
  √  Response time is less than 5s

Iteration 499/500

→ Create Document
  POST http://localhost:8181/documents [200 OK, 296B, 23ms]
  √  Status code is 200
  √  Response has accountId
  √  Response time is less than 5s

Iteration 500/500

→ Create Document
  POST http://localhost:8181/documents [200 OK, 267B, 12ms]
  √  Status code is 200
  √  Response has accountId
  √  Response time is less than 5s

┌─────────────────────────┬───────────────────┬───────────────────┐
│                         │          executed │            failed │
├─────────────────────────┼───────────────────┼───────────────────┤
│              iterations │               500 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│                requests │               500 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│            test-scripts │               500 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│      prerequest-scripts │               500 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│              assertions │              1500 │                 0 │
├─────────────────────────┴───────────────────┴───────────────────┤
│ total run duration: 2m 7.4s                                     │
├─────────────────────────────────────────────────────────────────┤
│ total data received: 60.5kB (approx)                            │
├─────────────────────────────────────────────────────────────────┤
│ average response time: 29ms [min: 10ms, max: 410ms, s.d.: 37ms] │