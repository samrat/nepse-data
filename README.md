# nepse-data

A data API that serves market information and stock prices from Nepal
Stock Exchange's website as JSON. The data is obtained by scraping web
pages.

**This is in alpha. Changes might be made to the API. Also, because
  this is scraped data, it will break with changes to NEPSE's website
  design.**

When the market is open(12:00 to 15:00 NPT), it scrapes the website
every 30 seconds and caches it to avoid scraping for every request.

## Usage

A running instance can be found at http://nepse-data.herokuapp.com .

To run your own copy, clone this repository and run:

    cd nepse-data
    lein run

## API endpoints

### `/market-status`
### `/live-data`
### `/market-info`
### `/last-trading-day`
### `/stock-details/:symbol`
Replace `:symbol` with the ticker symbol of the company(for example, ADBL)
### `/ninety-days-info/:symbol`
Replace `:symbol` with the ticker symbol of the company(for example, ADBL)

## TODO
- Add more docs
- Add examples
- Polish streaming via WebSockets
- Make /ninety-days-info more failure tolerant. The website seems to
  be irresponsive sometimes. (maybe use core.async to make parallel
  requests?)


## License

Copyright © 2013 Samrat Man Singh

Distributed under the Eclipse Public License, the same as Clojure.
